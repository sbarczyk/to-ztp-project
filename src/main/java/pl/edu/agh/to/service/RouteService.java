package pl.edu.agh.to.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.edu.agh.to.gtfs.statics.StaticGtfsService;
import pl.edu.agh.to.model.*;
import pl.edu.agh.to.model.Calendar; // Twój model

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RouteService {

    private final StaticGtfsService staticData;
    private final RandomDepartureService realtimeData;

    public RouteSearchResult findFastestConnection(String startName, String endName) {
        List<Stop> startStops = staticData.stopsByName.get(startName);
        List<Stop> endStops = staticData.stopsByName.get(endName);

        if (startStops == null || endStops == null) {
            throw new IllegalArgumentException("Unknown stop: " +
                    (startStops == null ? startName : endName));
        }

        Set<String> startIds = startStops.stream().map(Stop::getId).collect(Collectors.toSet());
        Set<String> endIds = endStops.stream().map(Stop::getId).collect(Collectors.toSet());

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        System.out.println("Searching route: " + startName + " -> " + endName);

        Map<String, Long> currentDelays = realtimeData.getCurrentDelays();
        List<RouteSearchResult> candidates = new ArrayList<>();

        for (Map.Entry<String, List<StopTime>> entry : staticData.stopTimesByTripId.entrySet()) {
            String tripId = entry.getKey();
            List<StopTime> schedule = entry.getValue();

            StopTime startInfo = null;
            StopTime endInfo = null;

            for (StopTime st : schedule) {
                if (startIds.contains(st.getStopId())) startInfo = st;
                if (endIds.contains(st.getStopId())) endInfo = st;
            }

            if (startInfo != null && endInfo != null && endInfo.getStopSequence() > startInfo.getStopSequence()) {

                Trip trip = staticData.tripsById.get(tripId);

                if (trip == null) {
                    continue;
                }

                if (isRunningToday(trip.getServiceId(), today)) {
                    long delay = currentDelays.getOrDefault(tripId, 0L);
                    LocalTime realDeparture = startInfo.getDepartureTime().plusSeconds(delay);
                    LocalTime realArrival = endInfo.getArrivalTime().plusSeconds(delay);

                    if (realDeparture.isAfter(now)) {
                        candidates.add(RouteSearchResult.builder()
                                .startStop(startName)
                                .endStop(endName)
                                .tripId(tripId)
                                .routeId(trip.getRouteId())
                                .departureTime(realDeparture)
                                .arrivalTime(realArrival)
                                .delayInSeconds(delay)
                                .build());
                    }
                }
            }
        }

        return candidates.stream()
                .min(Comparator.comparing(RouteSearchResult::getArrivalTime))
                .orElseThrow(() -> new RuntimeException("No direct connections!"));
    }

    private boolean isRunningToday(String serviceId, LocalDate date) {
        if (staticData.calendarDatesByServiceId.containsKey(serviceId)) {
            List<CalendarDate> exceptions = staticData.calendarDatesByServiceId.get(serviceId);
            for (CalendarDate cd : exceptions) {
                if (cd.getDate().equals(date)) {
                    return cd.getExceptionType() == 1;
                }
            }
        }

        Calendar cal = staticData.calendarsByServiceId.get(serviceId);
        if (cal == null) return false;

        if (date.isBefore(cal.getStartDate()) || date.isAfter(cal.getEndDate())) {
            return false;
        }

        return cal.getOperatingDays().contains(date.getDayOfWeek());
    }
}