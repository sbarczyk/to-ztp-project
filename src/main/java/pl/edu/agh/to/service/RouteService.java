package pl.edu.agh.to.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.to.model.*;
import pl.edu.agh.to.repository.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteService {

    // Wstrzykujemy Repozytoria
    private final StopRepository stopRepository;
    private final TripRepository tripRepository;
    private final StopTimeRepository stopTimeRepository;
    private final CalendarRepository calendarRepository;
    private final CalendarDateRepository calendarDateRepository;

    private final RandomDepartureService realtimeData;

    @Transactional(readOnly = true) // Przyspiesza odczyt z bazy
    public RouteSearchResult findFastestConnection(String startName, String endName) {
        log.info("Szukam trasy PostgreSQL: {} -> {}", startName, endName);

        // 1. Pobierz ID przystanków (jeden przystanek = wiele słupków/ID)
        List<String> startIds = stopRepository.findByName(startName)
                .stream().map(Stop::getId).toList();
        List<String> endIds = stopRepository.findByName(endName)
                .stream().map(Stop::getId).toList();

        if (startIds.isEmpty() || endIds.isEmpty()) {
            throw new IllegalArgumentException("Nie znaleziono przystanku o podanej nazwie.");
        }

        // 2. MAGICZNE ZAPYTANIE SQL
        // Baza sama znajduje Tripy, które przejeżdżają przez Start i Koniec w dobrej kolejności
        List<Trip> matchingTrips = tripRepository.findDirectConnections(startIds, endIds);

        log.info("Baza znalazła {} pasujących kursów (linii).", matchingTrips.size());

        List<RouteSearchResult> candidates = new ArrayList<>();

        // Parametry symulacji (możesz zmienić na LocalDate.now() jeśli dane są aktualne)
        LocalDate today = LocalDate.now();
//        LocalTime now = LocalTime.of(8, 0);
        LocalTime now = LocalTime.now();

        // Pobierz opóźnienia raz (zamiast w pętli)
        Map<String, Long> currentDelays = realtimeData.getCurrentDelays();

        for (Trip trip : matchingTrips) {
            // 3. Sprawdź Kalendarz (czy kursuje dzisiaj?)
            if (!isRunningToday(trip.getServiceId(), today)) {
                continue;
            }

            // 4. Pobierz czasy dla tego konkretnego kursu
            // Używamy repozytorium, aby pobrać tylko czasy dla tego jednego Tripa
            List<StopTime> times = stopTimeRepository.findByTripIdOrderByStopSequence(trip.getTripId());

            StopTime startInfo = null;
            StopTime endInfo = null;

            // Znajdź właściwe przystanki w liście
            for (StopTime st : times) {
                if (startIds.contains(st.getStopId())) startInfo = st;
                if (endIds.contains(st.getStopId())) endInfo = st;
            }

            if (startInfo != null && endInfo != null) {
                long delay = currentDelays.getOrDefault(trip.getTripId(), 0L);
                LocalTime realDeparture = startInfo.getDepartureTime().plusSeconds(delay);
                LocalTime realArrival = endInfo.getArrivalTime().plusSeconds(delay);

                // Sprawdź czy autobus już nie odjechał
                if (realDeparture.isAfter(now)) {
                    candidates.add(RouteSearchResult.builder()
                            .startStop(startName)
                            .endStop(endName)
                            .tripId(trip.getTripId())
                            .routeId(trip.getRouteId())
                            .departureTime(realDeparture)
                            .arrivalTime(realArrival)
                            .delayInSeconds(delay)
                            .build());
                }
            }
        }

        return candidates.stream()
                .min(Comparator.comparing(RouteSearchResult::getArrivalTime))
                .orElseThrow(() -> new RuntimeException("Brak połączeń bezpośrednich o tej porze."));
    }

    // W pliku RouteService.java

    private boolean isRunningToday(String serviceId, LocalDate date) {
        // A. Wyjątki (calendar_dates) - Pobieramy listę
        List<CalendarDate> exceptions = calendarDateRepository.findByServiceIdAndDate(serviceId, date);

        if (!exceptions.isEmpty()) {
            // Jeśli znaleziono jakiekolwiek wyjątki, bierzemy pierwszy z nich
            // (zakładamy, że jeśli są dwa, to mówią to samo)
            return exceptions.get(0).getExceptionType() == 1;
        }

        // B. Kalendarz regularny (bez zmian)
        return calendarRepository.findById(serviceId)
                .map(cal -> {
                    if (date.isBefore(cal.getStartDate()) || date.isAfter(cal.getEndDate())) return false;
                    return cal.getOperatingDays().contains(date.getDayOfWeek());
                })
                .orElse(false);
    }
}