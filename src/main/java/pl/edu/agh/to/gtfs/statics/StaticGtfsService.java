package pl.edu.agh.to.gtfs.statics;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.to.config.GtfsProperties;
import pl.edu.agh.to.model.Calendar;
import pl.edu.agh.to.model.CalendarDate;
import pl.edu.agh.to.model.Route;
import pl.edu.agh.to.model.Stop;
import pl.edu.agh.to.model.StopTime;
import pl.edu.agh.to.model.Trip;
import pl.edu.agh.to.repository.CalendarDateRepository;
import pl.edu.agh.to.repository.CalendarRepository;
import pl.edu.agh.to.repository.RouteRepository;
import pl.edu.agh.to.repository.StopRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Time;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

/**
 * Periodically downloads and imports GTFS Static feed into the database.
 * <p>
 * Small tables are persisted through JPA repositories for readability,
 * while large tables (trips, stop_times) are inserted with JdbcTemplate for performance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StaticGtfsService {

    @Value("${ztp.gtfs.jdbc.batch.trips:20000}")
    private int tripsBatchSize;

    @Value("${ztp.gtfs.jdbc.batch.stop-times:10000}")
    private int stopTimesBatchSize;

    private final StaticGtfsClient staticGtfsClient;
    private final GtfsZipExtractor zipExtractor;
    private final StaticGtfsParser parser;
    private final JdbcTemplate jdbcTemplate;
    private final GtfsProperties props;

    private final RouteRepository routeRepository;
    private final StopRepository stopRepository;
    private final CalendarRepository calendarRepository;
    private final CalendarDateRepository calendarDateRepository;

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

        if (!isUpdateNeeded(localZip, remoteTime)) {
            log.info("GTFS Static is up to date (remote={})", remoteTime);
            return;
        }

        log.info("GTFS Static update needed (remote={})", remoteTime);
        performReload(remoteTime);
    }

    private boolean isUpdateNeeded(Path localZip, Instant remoteTime) {
        try {
            if (!Files.exists(localZip) || isStopsTableEmpty()) {
                return true;
            }
            Instant localTime = Files.getLastModifiedTime(localZip).toInstant();
            return remoteTime.isAfter(localTime);
        } catch (IOException ex) {
            log.warn("Failed to read local GTFS timestamp, forcing update: {}", ex.getMessage());
            return true;
        }
    }

    private boolean isStopsTableEmpty() {
        Integer cnt = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stops", Integer.class);
        return cnt == null || cnt == 0;
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
        Instant start = Instant.now();
        log.info("GTFS import started (dir={})", dir.toAbsolutePath());

        clearGtfsTables();

        List<Route> routes = parser.parseRoutes(dir.resolve("routes.txt"));
        List<Stop> stops = parser.parseStops(dir.resolve("stops.txt"));
        List<Calendar> calendars = parser.parseCalendar(dir.resolve("calendar.txt"));
        List<CalendarDate> calendarDates = parser.parseCalendarDates(dir.resolve("calendar_dates.txt"));
        List<Trip> trips = parser.parseTrips(dir.resolve("trips.txt"));
        List<StopTime> stopTimes = parser.parseStopTimes(dir.resolve("stop_times.txt"));

        log.info("Parsed files: routes={}, stops={}, calendars={}, calendarDates={}, trips={}, stopTimes={}",
                routes.size(), stops.size(), calendars.size(), calendarDates.size(), trips.size(), stopTimes.size());

        saveIfNotEmpty("routes", routes, routeRepository::saveAll);
        saveIfNotEmpty("stops", stops, stopRepository::saveAll);
        saveIfNotEmpty("calendar", calendars, calendarRepository::saveAll);
        saveIfNotEmpty("calendar_dates", calendarDates, calendarDateRepository::saveAll);

        batchInsertTrips(trips);
        batchInsertStopTimes(stopTimes);

        Duration took = Duration.between(start, Instant.now());
        log.info("GTFS import finished successfully in {} ms", took.toMillis());
    }

    private void clearGtfsTables() {
        log.info("Clearing GTFS tables");
        jdbcTemplate.update("DELETE FROM stop_times");
        jdbcTemplate.update("DELETE FROM trips");
        jdbcTemplate.update("DELETE FROM routes");
        jdbcTemplate.update("DELETE FROM calendar_dates");
        jdbcTemplate.update("DELETE FROM calendar_operating_days");
        jdbcTemplate.update("DELETE FROM calendar");
        jdbcTemplate.update("DELETE FROM stops");
        log.info("GTFS tables cleared");
    }

    private <T> void saveIfNotEmpty(String name, List<T> items, Consumer<List<T>> saver) {
        if (items.isEmpty()) {
            log.info("Skipping {} insert (0 records)", name);
            return;
        }
        saver.accept(items);
        log.info("Inserted {}={}", name, items.size());
    }

    private void batchInsertTrips(List<Trip> trips) {
        if (trips.isEmpty()) {
            log.info("Skipping trips insert (0 records)");
            return;
        }

        String sql = """
                INSERT INTO trips (trip_id, route_id, service_id)
                VALUES (?, ?, ?)
                """;

        jdbcTemplate.batchUpdate(sql, trips, tripsBatchSize, (ps, t) -> {
            ps.setString(1, t.getTripId());
            ps.setString(2, t.getRouteId());
            ps.setString(3, t.getServiceId());
        });

        log.info("Inserted trips={}", trips.size());
    }

    private void batchInsertStopTimes(List<StopTime> stopTimes) {
        if (stopTimes.isEmpty()) {
            log.info("Skipping stop_times insert (0 records)");
            return;
        }

        String sql = """
                INSERT INTO stop_times (id, trip_id, stop_id, arrival_time, departure_time, stop_sequence)
                VALUES (nextval('stop_times_seq'), ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.batchUpdate(sql, stopTimes, stopTimesBatchSize, (ps, st) -> {
            ps.setString(1, st.getTripId());
            ps.setString(2, st.getStopId());
            ps.setTime(3, Time.valueOf(st.getArrivalTime()));
            ps.setTime(4, Time.valueOf(st.getDepartureTime()));
            ps.setInt(5, st.getStopSequence());
        });

        log.info("Inserted stop_times={}", stopTimes.size());
    }
}