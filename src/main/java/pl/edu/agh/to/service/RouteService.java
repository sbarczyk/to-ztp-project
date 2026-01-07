package pl.edu.agh.to.service;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.to.exceptions.BadRequestException;
import pl.edu.agh.to.exceptions.NotFoundException;
import pl.edu.agh.to.gtfs.realtime.GtfsDelayService;
import pl.edu.agh.to.model.CalendarDate;
import pl.edu.agh.to.model.Route;
import pl.edu.agh.to.model.RouteSearchResult;
import pl.edu.agh.to.model.Stop;
import pl.edu.agh.to.model.StopTime;
import pl.edu.agh.to.model.Trip;
import pl.edu.agh.to.repository.CalendarDateRepository;
import pl.edu.agh.to.repository.CalendarRepository;
import pl.edu.agh.to.repository.StopRepository;
import pl.edu.agh.to.repository.TripRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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
    public RouteSearchResult findFastestConnection(String startName, String endName) {
        String start = normalize(startName);
        String end = normalize(endName);

        log.info("Searching fastest direct connection: '{}' -> '{}'", start, end);

        List<String> startIds = stopRepository.findByName(start).stream()
                .map(Stop::getId)
                .toList();
        List<String> endIds = stopRepository.findByName(end).stream()
                .map(Stop::getId)
                .toList();

        if (startIds.isEmpty()) {
            throw new BadRequestException("Start stop not found: " + start);
        }
        if (endIds.isEmpty()) {
            throw new BadRequestException("End stop not found: " + end);
        }

        List<Object[]> results = tripRepository.findDirectConnectionsWithDetails(startIds, endIds);
        if (results.isEmpty()) {
            throw new NotFoundException("No direct connections found for given stops");
        }

        Map<String, Long> delays = fetchDelaysSafe();
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        List<RouteSearchResult> candidates = new ArrayList<>();

        for (Object[] row : results) {
            Trip trip = (Trip) row[0];
            StopTime stStart = (StopTime) row[1];
            StopTime stEnd = (StopTime) row[2];
            Route route = (Route) row[3];

            if (!isTripOperating(trip.getServiceId(), today)) {
                continue;
            }

            long delay = delays.getOrDefault(trip.getTripId(), 0L);

            LocalTime realDeparture = stStart.getDepartureTime().plusSeconds(delay);
            LocalTime realArrival = stEnd.getArrivalTime().plusSeconds(delay);

            if (!realDeparture.isAfter(now)) {
                continue;
            }

            String displayName = (route != null && route.getRouteShortName() != null && !route.getRouteShortName().isBlank())
                    ? route.getRouteShortName()
                    : "ID:" + trip.getRouteId();

            candidates.add(RouteSearchResult.builder()
                    .startStop(start)
                    .endStop(end)
                    .tripId(trip.getTripId())
                    .routeId(trip.getRouteId())
                    .routeShortName(displayName)
                    .departureTime(realDeparture)
                    .arrivalTime(realArrival)
                    .delayInSeconds(delay)
                    .build());
        }

        return candidates.stream()
                .min(Comparator.comparing(RouteSearchResult::getArrivalTime))
                .orElseThrow(() -> new NotFoundException("No direct connections available for today"));
    }

    private Map<String, Long> fetchDelaysSafe() {
        try {
            return delayService.getCurrentDelays();
        } catch (InvalidProtocolBufferException ex) {
            log.warn("Delays unavailable due to protobuf parsing error: {}", ex.getMessage());
            return Map.of();
        }
    }

    private boolean isTripOperating(String serviceId, LocalDate date) {
        List<CalendarDate> exceptions = calendarDateRepository.findByServiceIdAndDate(serviceId, date);
        if (!exceptions.isEmpty()) {
            return exceptions.get(0).getExceptionType() == 1;
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