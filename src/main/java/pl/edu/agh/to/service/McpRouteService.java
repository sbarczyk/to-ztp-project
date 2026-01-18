package pl.edu.agh.to.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.to.gtfs.realtime.GtfsDelayService;
import pl.edu.agh.to.model.*;
import pl.edu.agh.to.repository.CalendarDateRepository;
import pl.edu.agh.to.repository.CalendarRepository;
import pl.edu.agh.to.repository.StopRepository;
import pl.edu.agh.to.repository.StopTimeRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class McpRouteService {

    private final StopTimeRepository stopTimeRepository;
    private final CalendarRepository calendarRepository;
    private final CalendarDateRepository calendarDateRepository;
    private final GtfsDelayService delayService;
    private final StopRepository stopRepository; // <-- WAŻNE: Wstrzykujemy repozytorium przystanków

    @Transactional(readOnly = true)
    public List<ConnectionDto> findTop3Connections(String startStop, String endStop, LocalTime startTime) {
        // Używamy log.error, żeby na 100% zobaczyć to w terminalu (nawet na czerwono)
        log.error(">>> [START] Szukam połączenia: {} -> {}", startStop, endStop);

        // 0. SPRAWDZENIE CZY BAZA JEST PUSTA
        long stopsCount = stopRepository.count();
        log.error(">>> [INFO] Liczba przystanków w bazie: {}", stopsCount);

        if (stopsCount == 0) {
            log.error(">>> [ALARM] Baza danych jest pusta! Import nie zadziałał.");
            return List.of();
        }

        // KROK 1: ID
        List<String> startIds = getStopIds(startStop);
        List<String> endIds = getStopIds(endStop);
        log.error(">>> [INFO] ID Startowe: {}", startIds);
        log.error(">>> [INFO] ID Końcowe: {}", endIds);

        if (startIds.isEmpty() || endIds.isEmpty()) {
            log.error(">>> [KONIEC] Nie znaleziono przystanków o podanych nazwach.");
            return List.of();
        }

        // KROK 2: Kalendarz
        LocalDate today = LocalDate.now();
        List<String> activeServices = getActiveServiceIds(today);
        log.error(">>> [INFO] Aktywne serwisy dzisiaj ({}): {}", today, activeServices.size());

        Map<String, Long> delays = delayService.getCurrentDelays();

        // KROK 3: Zapytanie
        log.error(">>> [SQL] Odpytuję bazę...");
        long startT = System.currentTimeMillis();

        List<Object[]> results = stopTimeRepository.findNextDepartures(
                startIds,
                endIds,
                startTime,
                activeServices,
                PageRequest.of(0, 3)
        );

        log.error(">>> [SQL] Wynik w {} ms. Znaleziono: {}", System.currentTimeMillis() - startT, results.size());

        return results.stream()
                .map(row -> mapToDto(row, delays))
                .toList();
    }

    // Metoda pomocnicza do wyciągania ID po nazwie
    private List<String> getStopIds(String name) {
        return stopRepository.findByName(name).stream()
                .map(Stop::getId)
                .toList();
    }

    // --- Reszta metod (mapToDto, getActiveServiceIds) pozostaje bez zmian ---
    // (Skopiuj je ze swojej starej wersji lub poprzednich odpowiedzi)

    private ConnectionDto mapToDto(Object[] row, Map<String, Long> delays) {
        StopTime stA = (StopTime) row[0];
        StopTime stB = (StopTime) row[1];
        Trip trip = (Trip) row[2];
        Route route = (Route) row[3];

        long delaySeconds = delays.getOrDefault(trip.getTripId(), 0L);
        int delayMinutes = (int) (delaySeconds / 60);

        LocalTime scheduledDeparture = stA.getDepartureTime();
        LocalTime predictedDeparture = scheduledDeparture.plusSeconds(delaySeconds);

        // Pamiętaj o obsłudze braku headsign (jeśli parser wstawia null)
//        String direction = (trip.getHeadsign() != null) ? trip.getHeadsign() : stB.getStopId();
        String direction = stB.getStopId();
        return new ConnectionDto(
                route.getRouteShortName(),
                direction,
                stA.getStopId(), // Możesz tu wstawić nazwę, jeśli wyciągniesz ją z StopRepository, ale ID wystarczy
                stB.getStopId(),
                scheduledDeparture,
                stB.getArrivalTime(),
                delayMinutes,
                predictedDeparture
        );
    }

    private List<String> getActiveServiceIds(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        List<String> standardServices = calendarRepository.findAll().stream()
                .filter(c -> !date.isBefore(c.getStartDate()) && !date.isAfter(c.getEndDate()))
                .filter(c -> c.operatesOn(dayOfWeek))
                .map(Calendar::getServiceId)
                .collect(Collectors.toList());

        List<CalendarDate> exceptions = calendarDateRepository.findAll().stream()
                .filter(cd -> cd.getDate().equals(date))
                .toList();

        for (CalendarDate ex : exceptions) {
            if (ex.getExceptionType() == 1) {
                if (!standardServices.contains(ex.getServiceId())) standardServices.add(ex.getServiceId());
            } else if (ex.getExceptionType() == 2) {
                standardServices.remove(ex.getServiceId());
            }
        }
        return standardServices;
    }
}