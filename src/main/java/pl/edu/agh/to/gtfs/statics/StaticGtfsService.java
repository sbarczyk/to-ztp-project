package pl.edu.agh.to.gtfs.statics;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.to.config.GtfsProperties;
import pl.edu.agh.to.model.*;
import pl.edu.agh.to.repository.CalendarDateRepository;
import pl.edu.agh.to.repository.CalendarRepository;
import pl.edu.agh.to.repository.RouteRepository;
import pl.edu.agh.to.repository.StopRepository;
import pl.edu.agh.to.repository.TripRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Time;
import java.time.Duration;
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

    /**
     * Imports GTFS Static data into the database.
     * Uses JdbcTemplate for bulk delete/insert for performance and predictability on large datasets (e.g. stop_times ~1.5M rows).
     * JPA repositories are used for application queries and domain logic.
     */
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

        batchInsertRoutes(routes);
        batchInsertStops(stops);
        batchInsertCalendars(calendars);
        batchInsertCalendarDates(calendarDates);
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

    private void batchInsertRoutes(List<Route> routes) {
        if (routes.isEmpty()) {
            log.info("Skipping routes insert (0 records)");
            return;
        }

        String sql = """
            INSERT INTO routes (route_id, route_short_name, route_long_name, route_type)
            VALUES (?, ?, ?, ?)
            """;

        jdbcTemplate.batchUpdate(sql, routes, 1000, (ps, r) -> {
            ps.setString(1, r.getRouteId());
            ps.setString(2, r.getRouteShortName());
            ps.setString(3, r.getRouteLongName());
            ps.setInt(4, r.getRouteType());
        });

        log.info("Inserted routes={}", routes.size());
    }

    private void batchInsertStops(List<Stop> stops) {
        if (stops.isEmpty()) {
            log.info("Skipping stops insert (0 records)");
            return;
        }

        String sql = """
            INSERT INTO stops (stop_id, stop_name, stop_lat, stop_lon)
            VALUES (?, ?, ?, ?)
            """;

        jdbcTemplate.batchUpdate(sql, stops, 2000, (ps, s) -> {
            ps.setString(1, s.getId());
            ps.setString(2, s.getName());
            ps.setDouble(3, s.getLat());
            ps.setDouble(4, s.getLon());
        });

        log.info("Inserted stops={}", stops.size());
    }

    private void batchInsertCalendars(List<Calendar> calendars) {
        if (calendars.isEmpty()) {
            log.info("Skipping calendar insert (0 records)");
            return;
        }

        String calSql = """
            INSERT INTO calendar (service_id, start_date, end_date)
            VALUES (?, ?, ?)
            """;

        jdbcTemplate.batchUpdate(calSql, calendars, 1000, (ps, c) -> {
            ps.setString(1, c.getServiceId());
            ps.setObject(2, c.getStartDate());
            ps.setObject(3, c.getEndDate());
        });

        List<CalendarDayRow> rows = calendars.stream()
                .flatMap(c -> c.getOperatingDays().stream()
                        .map(d -> new CalendarDayRow(c.getServiceId(), d.name())))
                .toList();

        if (!rows.isEmpty()) {
            String daysSql = """
                INSERT INTO calendar_operating_days (service_id, day)
                VALUES (?, ?)
                """;

            jdbcTemplate.batchUpdate(daysSql, rows, 2000, (ps, row) -> {
                ps.setString(1, row.serviceId());
                ps.setString(2, row.day());
            });
        }

        log.info("Inserted calendar records={}, operatingDays={}", calendars.size(), rows.size());
    }

    private record CalendarDayRow(String serviceId, String day) { }

    private void batchInsertCalendarDates(List<CalendarDate> calendarDates) {
        if (calendarDates.isEmpty()) {
            log.info("Skipping calendar_dates insert (0 records)");
            return;
        }

        String sql = """
            INSERT INTO calendar_dates (service_id, date, exception_type)
            VALUES (?, ?, ?)
            """;

        jdbcTemplate.batchUpdate(sql, calendarDates, 2000, (ps, cd) -> {
            ps.setString(1, cd.getServiceId());
            ps.setObject(2, cd.getDate());
            ps.setInt(3, cd.getExceptionType());
        });

        log.info("Inserted calendar_dates={}", calendarDates.size());
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

        jdbcTemplate.batchUpdate(sql, trips, 2000, (ps, t) -> {
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

        jdbcTemplate.batchUpdate(sql, stopTimes, 5000, (ps, st) -> {
            ps.setString(1, st.getTripId());
            ps.setString(2, st.getStopId());
            ps.setTime(3, Time.valueOf(st.getArrivalTime()));
            ps.setTime(4, Time.valueOf(st.getDepartureTime()));
            ps.setInt(5, st.getStopSequence());
        });

        log.info("Inserted stop_times={}", stopTimes.size());
    }
}