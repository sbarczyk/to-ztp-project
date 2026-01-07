package pl.edu.agh.to.gtfs.statics;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.to.model.Stop;
import pl.edu.agh.to.model.StopTime;
import pl.edu.agh.to.model.Trip;
import pl.edu.agh.to.repository.CalendarDateRepository;
import pl.edu.agh.to.repository.CalendarRepository;
import pl.edu.agh.to.repository.RouteRepository;
import pl.edu.agh.to.repository.StopRepository;
import pl.edu.agh.to.repository.StopTimeRepository;
import pl.edu.agh.to.repository.TripRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Time;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaticGtfsService {

    private final StaticGtfsClient staticGtfsClient;
    private final GtfsZipExtractor zipExtractor;
    private final StaticGtfsParser parser;
    private final JdbcTemplate jdbcTemplate;

    private final StopRepository stopRepository;
    private final TripRepository tripRepository;
    private final StopTimeRepository stopTimeRepository;
    private final CalendarRepository calendarRepository;
    private final CalendarDateRepository calendarDateRepository;
    private final RouteRepository routeRepository;

    @Value("${ztp.gtfs.data-dir}")
    private String dataDir;

    @Value("${ztp.gtfs.static.file}")
    private String staticFile;

    @PostConstruct
    public void init() {
        refreshDataIfNeeded();
    }

    @Scheduled(fixedRateString = "${ztp.gtfs.check-interval-ms:3600000}")
    public void scheduledRefresh() {
        log.info("Cyclycal check for GTFS Static updates initiated.");
        refreshDataIfNeeded();
    }

    public synchronized void refreshDataIfNeeded() {
        Instant remoteTime = staticGtfsClient.getRemoteLastModified();
        Path localZip = Path.of(dataDir, staticFile);

        boolean updateNeeded;
        try {
            if (!Files.exists(localZip) || stopRepository.count() == 0) {
                updateNeeded = true;
            } else {
                Instant localTime = Files.getLastModifiedTime(localZip).toInstant();
                updateNeeded = remoteTime.isAfter(localTime);
            }
        } catch (IOException e) {
            updateNeeded = true;
        }

        if (updateNeeded) {
            log.info("Updating GTFS database. Remote version: {}", remoteTime);
            performReload(remoteTime);
        } else {
            log.info("GTFS database is up to date.");
        }
    }

    private void performReload(Instant remoteTime) {
        try {
            Path zipPath = staticGtfsClient.downloadZipToDisk(remoteTime);
            String datasetName = staticFile.replace(".zip", "");
            Path targetDir = Path.of(dataDir, "extracted", datasetName);
            zipExtractor.extractZipToDirectory(zipPath, targetDir);
            importToDatabase(targetDir);
        } catch (Exception e) {
            log.error("Failed to reload GTFS Static data: {}", e.getMessage());
        }
    }

    @Transactional
    protected void importToDatabase(Path dir) throws IOException {
        log.info("Deleting old GTFS records...");
        stopTimeRepository.deleteAllInBatch();
        tripRepository.deleteAllInBatch();
        routeRepository.deleteAllInBatch();
        calendarDateRepository.deleteAllInBatch();
        calendarRepository.deleteAllInBatch();
        stopRepository.deleteAllInBatch();

        // UWAGA: Zakładamy, że parser ma metodę parseRoutes (jeśli nie, należy ją dodać analogicznie do innych)
        // log.info("Importing routes...");
        // routeRepository.saveAll(parser.parseRoutes(dir.resolve("routes.txt")));

        log.info("Importing stops, calendars and trips...");
        stopRepository.saveAll(parser.parseStops(dir.resolve("stops.txt")));
        calendarRepository.saveAll(parser.parseCalendar(dir.resolve("calendar.txt")));
        calendarDateRepository.saveAll(parser.parseCalendarDates(dir.resolve("calendar_dates.txt")));
        tripRepository.saveAll(parser.parseTrips(dir.resolve("trips.txt")));

        log.info("Starting batch import of StopTimes...");
        List<StopTime> stopTimes = parser.parseStopTimes(dir.resolve("stop_times.txt"));
        String sql = "INSERT INTO stop_times (id, trip_id, stop_id, arrival_time, departure_time, stop_sequence) " +
                "VALUES (nextval('stop_times_seq'), ?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, stopTimes, 1000, (ps, st) -> {
            ps.setString(1, st.getTripId());
            ps.setString(2, st.getStopId());
            ps.setTime(3, Time.valueOf(st.getArrivalTime()));
            ps.setTime(4, Time.valueOf(st.getDepartureTime()));
            ps.setInt(5, st.getStopSequence());
        });
        log.info("Import process finished successfully.");
    }
}