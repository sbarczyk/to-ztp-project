package pl.edu.agh.to.gtfs.statics;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.to.model.*;
import pl.edu.agh.to.model.Calendar;
import pl.edu.agh.to.repository.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaticGtfsService {

    private final StaticGtfsClient staticGtfsClient;
    private final GtfsZipExtractor zipExtractor;
    private final StaticGtfsParser parser;

    // Repozytoria
    private final StopRepository stopRepository;
    private final TripRepository tripRepository;
    private final StopTimeRepository stopTimeRepository;
    private final CalendarRepository calendarRepository;
    private final CalendarDateRepository calendarDateRepository;

    @Value("${ztp.gtfs.data-dir:data}")
    private String dataDir;

    @Value("${ztp.gtfs.static.file}")
    private String staticFile;

    @PostConstruct
    @Transactional
    public void init() {
        // 1. Sprawdź cache - jeśli baza ma dane, nie ładuj ponownie
        if (stopRepository.count() > 0) {
            log.info("Dane GTFS znalezione w bazie PostgreSQL. Pomijam ładowanie.");
            return;
        }

        log.info("Baza pusta. Rozpoczynam pobieranie i import WSZYSTKICH danych GTFS...");

        // 2. Pobierz plik ZIP zdefiniowany w application.properties
        Path zipPath = staticGtfsClient.downloadZipToDisk();

        // 3. Wypakuj do stałego katalogu
        String datasetName = staticFile.replace(".zip", "");
        Path targetDir = Path.of(dataDir, "extracted", datasetName);

        log.info("--> Przetwarzanie zbioru: {}", datasetName);

        zipExtractor.extractZipToDirectory(zipPath, targetDir);

        // 4. Załaduj do bazy
        try {
            loadDataToDb(targetDir, datasetName);
        } catch (IOException e) {
            log.error("Błąd podczas importu zbioru {}", datasetName, e);
        }

        log.info("=== ZAKOŃCZONO CAŁKOWITY IMPORT DANYCH DO BAZY ===");
    }

    private void loadDataToDb(Path dir, String datasetName) throws IOException {
        log.info("[{}] Importowanie PRZYSTANKÓW...", datasetName);
        List<Stop> stops = parser.parseStops(dir.resolve("stops.txt"));
        stopRepository.saveAll(stops);

        log.info("[{}] Importowanie KALENDARZA...", datasetName);
        List<Calendar> calendars = parser.parseCalendar(dir.resolve("calendar.txt"));
        calendarRepository.saveAll(calendars);

        log.info("[{}] Importowanie WYJĄTKÓW KALENDARZA...", datasetName);
        List<CalendarDate> dates = parser.parseCalendarDates(dir.resolve("calendar_dates.txt"));
        calendarDateRepository.saveAll(dates);

        log.info("[{}] Importowanie KURSÓW (Trips)...", datasetName);
        List<Trip> trips = parser.parseTrips(dir.resolve("trips.txt"));
        tripRepository.saveAll(trips);

        log.info("[{}] Importowanie CZASÓW (StopTimes) - to potrwa...", datasetName);
        List<StopTime> stopTimes = parser.parseStopTimes(dir.resolve("stop_times.txt"));

        // Batch insert dla wydajności
        int batchSize = 10000;
        for (int i = 0; i < stopTimes.size(); i += batchSize) {
            int end = Math.min(i + batchSize, stopTimes.size());
            stopTimeRepository.saveAll(stopTimes.subList(i, end));

            if (i % 50000 == 0) {
                log.info("[{}] Zapisano {} / {} czasów...", datasetName, i, stopTimes.size());
            }
        }
        log.info("[{}] Zakończono import tego zbioru.", datasetName);
    }
}