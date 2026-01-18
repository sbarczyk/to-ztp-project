package pl.edu.agh.to.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.to.gtfs.realtime.GtfsDelayService;
import pl.edu.agh.to.model.DeparturesResponseDto;
import pl.edu.agh.to.model.NextDepartureDto;
import pl.edu.agh.to.model.Stop;
import pl.edu.agh.to.model.StopTime;
import pl.edu.agh.to.model.Trip;
import pl.edu.agh.to.repository.StopRepository;
import pl.edu.agh.to.repository.StopTimeRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class McpDeparturesService {

    private final StopTimeRepository stopTimeRepository;
    private final StopRepository stopRepository;
    private final ActiveServiceResolver activeServiceResolver;
    private final GtfsDelayService delayService;

    @Transactional(readOnly = true)
    public DeparturesResponseDto getNext5Departures(
            String stopName,
            String lineNumber,
            LocalDate date,
            LocalTime time
    ) {
        LocalDate queryDate = (date != null) ? date : LocalDate.now();
        LocalTime queryTime = (time != null) ? time : LocalTime.now();

        String stop = normalize(stopName);
        String line = normalize(lineNumber);

        log.info("Finding next 5 departures: stop='{}', line='{}' at {} {}", stop, line, queryDate, queryTime);

        if (stop == null || line == null) {
            return new DeparturesResponseDto(stopName, lineNumber, queryDate, queryTime, List.of());
        }

        List<String> stopIds = findStopIds(stop);
        if (stopIds.isEmpty()) {
            log.warn("Stop not found: '{}'", stop);
            return new DeparturesResponseDto(stop, line, queryDate, queryTime, List.of());
        }

        List<String> activeServices = activeServiceResolver.getActiveServiceIds(queryDate);
        Map<String, Long> delays = delayService.getCurrentDelays();

        List<Object[]> rows = stopTimeRepository.findNextDeparturesForLine(
                stopIds,
                line,
                queryTime,
                activeServices,
                PageRequest.of(0, 5)
        );

        List<NextDepartureDto> departures = rows.stream()
                .map(row -> mapToDeparture(row, delays))
                .toList();

        return new DeparturesResponseDto(stop, line, queryDate, queryTime, departures);
    }

    private List<String> findStopIds(String stopName) {
        return stopRepository.findByName(stopName).stream()
                .map(Stop::getId)
                .toList();
    }

    private NextDepartureDto mapToDeparture(Object[] row, Map<String, Long> delays) {
        StopTime st = (StopTime) row[0];
        Trip trip = (Trip) row[1];

        long delaySeconds = delays.getOrDefault(trip.getTripId(), 0L);

        LocalTime scheduled = st.getDepartureTime();
        LocalTime predicted = scheduled.plusSeconds(delaySeconds);

        return new NextDepartureDto(
                scheduled,
                predicted,
                delaySeconds
        );
    }

    private String normalize(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }
}