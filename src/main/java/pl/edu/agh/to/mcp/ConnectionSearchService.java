package pl.edu.agh.to.mcp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.to.dbresults.FindNextDeparturesResult;
import pl.edu.agh.to.gtfs.realtime.GtfsDelayService;
import pl.edu.agh.to.dto.ConnectionDto;
import pl.edu.agh.to.dto.ConnectionsResponseDto;
import pl.edu.agh.to.model.Stop;
import pl.edu.agh.to.repository.StopRepository;
import pl.edu.agh.to.repository.StopTimeRepository;
import pl.edu.agh.to.service.ActiveServiceResolver;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectionSearchService {

    private final StopTimeRepository stopTimeRepository;
    private final StopRepository stopRepository;
    private final ActiveServiceResolver activeServiceResolver;
    private final GtfsDelayService delayService;

    @Transactional(readOnly = true)
    public ConnectionsResponseDto findTop3Connections(
            String fromStop,
            String toStop,
            LocalDate date,
            LocalTime time
    ) {
        LocalDate queryDate = (date != null) ? date : LocalDate.now();
        LocalTime queryTime = (time != null) ? time : LocalTime.now();

        String from = McpUtils.normalize(fromStop);
        String to = McpUtils.normalize(toStop);

        log.info("Finding top 3 connections: '{}' -> '{}' at {} {}", from, to, queryDate, queryTime);

        if (from == null || to == null) {
            return new ConnectionsResponseDto(fromStop, toStop, queryDate, queryTime, List.of());
        }

        List<String> fromIds = findStopIds(from);
        List<String> toIds = findStopIds(to);

        if (fromIds.isEmpty() || toIds.isEmpty()) {
            log.warn("Stop not found: from='{}', to='{}'", from, to);
            return new ConnectionsResponseDto(from, to, queryDate, queryTime, List.of());
        }

        List<String> activeServices = activeServiceResolver.getActiveServiceIds(queryDate);
        Map<String, Long> delays = delayService.getCurrentDelays();

        List<FindNextDeparturesResult> nextDeparturesResults = stopTimeRepository.findNextDepartures(
                fromIds,
                toIds,
                queryTime,
                activeServices,
                PageRequest.of(0, 3)
        );

        List<ConnectionDto> connections = nextDeparturesResults.stream()
                .map(result -> mapToConnection(result, delays))
                .toList();

        return new ConnectionsResponseDto(from, to, queryDate, queryTime, connections);
    }

    private List<String> findStopIds(String stopName) {
        return stopRepository.findByName(stopName).stream()
                .map(Stop::getId)
                .toList();
    }

    private ConnectionDto mapToConnection(FindNextDeparturesResult result, Map<String, Long> delays) {
        long delaySeconds = delays.getOrDefault(result.trip().getTripId(), 0L);

        LocalTime scheduledDeparture = result.stopTimeA().getDepartureTime();
        LocalTime predictedDeparture = scheduledDeparture.plusSeconds(delaySeconds);
        LocalTime predictedArrival = result.stopTimeB().getArrivalTime().plusSeconds(delaySeconds);

        return new ConnectionDto(
                result.route().getRouteShortName(),
                scheduledDeparture,
                predictedDeparture,
                predictedArrival,
                delaySeconds
        );
    }
}