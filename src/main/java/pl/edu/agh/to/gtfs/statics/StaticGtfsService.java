package pl.edu.agh.to.gtfs.statics;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.to.config.GtfsProperties;
import pl.edu.agh.to.model.StopTime;
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
    private final GtfsProperties props;

    private final StopRepository stopRepository;
    private final TripRepository tripRepository;
    private final StopTimeRepository stopTimeRepository;
    private final CalendarRepository calendarRepository;
    private final CalendarDateRepository calendarDateRepository;
    private final RouteRepository routeRepository;

    @PostConstruct
    public void init() {
        refreshDataIfNeeded();
    }

    @Scheduled(fixedRateString = "${ztp.gtfs.check-interval-ms:3600000}")
    public void scheduledRefresh() {
        log.info("Scheduled GTFS Static check started");
        refreshDataIfNeeded();
    }

    public synchronized void refreshDataIfNeeded() {
        Instant remoteTime = staticGtfsClient.getRemoteLastModified();
        Path localZip = Path.of(props.statics().dataDir(), props.statics().file());

        boolean updateNeeded = isUpdateNeeded(localZip, remoteTime);

        if (!updateNeeded) {
            log.info("GTFS Static is up to date (remote={})", remoteTime);
            return;
        }

        log.info("GTFS Static update needed (remote={})", remoteTime);
        performReload(remoteTime);
    }

    private boolean isUpdateNeeded(Path localZip, Instant remoteTime) {
        try {
            if (!Files.exists(localZip) || stopRepository.count() == 0) {
                return true;
            }
            Instant localTime = Files.getLastModifiedTime(localZip).toInstant();
            return remoteTime.isAfter(localTime);
        } catch (IOException ex) {
            log.warn("Failed to read local GTFS timestamp, forcing update: {}", ex.getMessage());
            return true;
        }
    }

    private void performReload(Instant remoteTime) {
        try {
            Path zipPath = staticGtfsClient.downloadZipToDisk(remoteTime);

            String datasetName = props.statics().file().replace(".zip", "");
            Path targetDir = Path.of(props.statics().dataDir(), "extracted", datasetName);

            log.info("Extracting GTFS zip to {}", targetDir.toAbsolutePath());
            zipExtractor.extractZipToDirectory(zipPath, targetDir);

            importToDatabase(targetDir);

            log.info("GTFS Static reload finished successfully");
        } catch (Exception ex) {
            log.error("Failed to reload GTFS Static data: {}", ex.getMessage(), ex);
        }
    }

    @Transactional
    protected void importToDatabase(Path dir) throws IOException {
        log.info("Deleting old GTFS records");
        stopTimeRepository.deleteAllInBatch();
        tripRepository.deleteAllInBatch();
        routeRepository.deleteAllInBatch();
        calendarDateRepository.deleteAllInBatch();
        calendarRepository.deleteAllInBatch();
        stopRepository.deleteAllInBatch();

        log.info("Importing routes");
        routeRepository.saveAll(parser.parseRoutes(dir.resolve("routes.txt")));

        log.info("Importing stops");
        stopRepository.saveAll(parser.parseStops(dir.resolve("stops.txt")));

        log.info("Importing calendars and calendar_dates");
        calendarRepository.saveAll(parser.parseCalendar(dir.resolve("calendar.txt")));
        calendarDateRepository.saveAll(parser.parseCalendarDates(dir.resolve("calendar_dates.txt")));

        log.info("Importing trips");
        tripRepository.saveAll(parser.parseTrips(dir.resolve("trips.txt")));

        log.info("Batch inserting stop_times");
        List<StopTime> stopTimes = parser.parseStopTimes(dir.resolve("stop_times.txt"));

        String sql = """
                INSERT INTO stop_times (id, trip_id, stop_id, arrival_time, departure_time, stop_sequence)
                VALUES (nextval('stop_times_seq'), ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.batchUpdate(sql, stopTimes, 1000, (ps, st) -> {
            ps.setString(1, st.getTripId());
            ps.setString(2, st.getStopId());
            ps.setTime(3, Time.valueOf(st.getArrivalTime()));
            ps.setTime(4, Time.valueOf(st.getDepartureTime()));
            ps.setInt(5, st.getStopSequence());
        });

        log.info("Import done: stops={}, routes={}, trips={}, stopTimes={}",
                stopRepository.count(), routeRepository.count(), tripRepository.count(), stopTimes.size());
    }
}