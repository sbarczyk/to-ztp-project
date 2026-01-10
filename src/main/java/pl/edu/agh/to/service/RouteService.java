package pl.edu.agh.to.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.to.exceptions.BadRequestException;
import pl.edu.agh.to.exceptions.NotFoundException;
import pl.edu.agh.to.gtfs.realtime.GtfsDelayService;
import pl.edu.agh.to.model.CalendarDate;
import pl.edu.agh.to.model.Route;
import pl.edu.agh.to.model.RouteSearchResultDto;
import pl.edu.agh.to.model.Stop;
import pl.edu.agh.to.model.StopTime;
import pl.edu.agh.to.model.Trip;
import pl.edu.agh.to.repository.CalendarDateRepository;
import pl.edu.agh.to.repository.CalendarRepository;
import pl.edu.agh.to.repository.StopRepository;
import pl.edu.agh.to.repository.TripRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteService {

    private final StopRepository stopRepository;
    private final TripRepository tripRepository;
    private final CalendarRepository calendarRepository;
    private final CalendarDateRepository calendarDateRepository;
    private final GtfsDelayService delayService;

    @Transactional(readOnly = true)
    public RouteSearchResultDto findFastestConnection(String startName, String endName) {
        String start = normalize(startName);
        String end = normalize(endName);

        log.info("Searching fastest direct connection: '{}' -> '{}'", start, end);

        List<String> startIds = getStopIdsByName(start);
        List<String> endIds = getStopIdsByName(end);

        validateStopPresence(start, startIds, end, endIds);

        List<Object[]> results = tripRepository.findDirectConnectionsWithDetails(startIds, endIds);

        Map<String, Long> delays = delayService.getCurrentDelays();
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        return results.stream()
                .map(row -> mapToCandidate(row, start, end, today, now, delays))
                .flatMap(Optional::stream)
                .min(Comparator.comparing(RouteSearchResultDto::getArrivalTime))
                .orElseThrow(() -> new NotFoundException("No direct connections available for today"));
    }

    private Optional<RouteSearchResultDto> mapToCandidate(Object[] row, String start, String end,
                                                          LocalDate today, LocalTime now, Map<String, Long> delays) {
        Trip trip = (Trip) row[0];
        StopTime stStart = (StopTime) row[1];
        StopTime stEnd = (StopTime) row[2];
        Route route = (Route) row[3];

        if (!isTripOperating(trip.getServiceId(), today)) {
            return Optional.empty();
        }

        long delay = delays.getOrDefault(trip.getTripId(), 0L);
        LocalTime realDeparture = stStart.getDepartureTime().plusSeconds(delay);
        LocalTime realArrival = stEnd.getArrivalTime().plusSeconds(delay);

        if (!realDeparture.isAfter(now)) {
            return Optional.empty();
        }

        return Optional.of(RouteSearchResultDto.builder()
                .startStop(start)
                .endStop(end)
                .tripId(trip.getTripId())
                .routeId(trip.getRouteId())
                .routeShortName(route.getRouteShortName())
                .departureTime(realDeparture)
                .arrivalTime(realArrival)
                .delayInSeconds(delay)
                .build());
    }

    private List<String> getStopIdsByName(String name) {
        return stopRepository.findByName(name).stream()
                .map(Stop::getId)
                .toList();
    }

    private void validateStopPresence(String start, List<String> startIds, String end, List<String> endIds) {
        if (startIds.isEmpty()) {
            throw new BadRequestException("Start stop not found: " + start);
        }
        if (endIds.isEmpty()) {
            throw new BadRequestException("End stop not found: " + end);
        }
    }

    private boolean isTripOperating(String serviceId, LocalDate date) {
        List<CalendarDate> exceptions = calendarDateRepository.findByServiceIdAndDate(serviceId, date);
        if (!exceptions.isEmpty()) {
            return exceptions.getFirst().getExceptionType() == 1;
        }

        return calendarRepository.findById(serviceId)
                .map(cal -> !date.isBefore(cal.getStartDate())
                        && !date.isAfter(cal.getEndDate())
                        && cal.getOperatingDays().contains(date.getDayOfWeek()))
                .orElse(false);
    }

    private String normalize(String value) {
        if (value == null) {
            throw new BadRequestException("Stop name must not be null");
        }
        String out = value.trim();
        if (out.isEmpty()) {
            throw new BadRequestException("Stop name must not be blank");
        }
        return out;
    }
}