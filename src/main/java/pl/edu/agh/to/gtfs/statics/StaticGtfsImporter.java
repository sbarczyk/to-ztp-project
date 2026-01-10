package pl.edu.agh.to.gtfs.statics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.to.config.GtfsProperties;
import pl.edu.agh.to.model.*;
import pl.edu.agh.to.repository.CalendarDateRepository;
import pl.edu.agh.to.repository.CalendarRepository;
import pl.edu.agh.to.repository.RouteRepository;
import pl.edu.agh.to.repository.StopRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Time;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
class StaticGtfsImporter {

    private final StaticGtfsParser parser;
    private final JdbcTemplate jdbcTemplate;

    private final GtfsProperties props;

    private final RouteRepository routeRepository;
    private final StopRepository stopRepository;
    private final CalendarRepository calendarRepository;
    private final CalendarDateRepository calendarDateRepository;

    @Transactional
    void importToDatabase(Path dir) throws IOException {
        Instant start = Instant.now();
        log.info("GTFS import started (dir={})", dir.toAbsolutePath());

        clearGtfsTables();

        GtfsProperties.StaticProperties.FilenameProperties filenames = props.statics().filenames();

        List<Route> routes = parser.parseRoutes(dir.resolve(filenames.routes()));
        List<Stop> stops = parser.parseStops(dir.resolve(filenames.stops()));
        List<Calendar> calendars = parser.parseCalendar(dir.resolve(filenames.calendar()));
        List<CalendarDate> calendarDates = parser.parseCalendarDates(dir.resolve(filenames.calendarDates()));
        List<Trip> trips = parser.parseTrips(dir.resolve(filenames.trips()));
        List<StopTime> stopTimes = parser.parseStopTimes(dir.resolve(filenames.stopTimes()));

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

        jdbcTemplate.batchUpdate(sql, trips, props.jdbcBatch().trips(), (ps, t) -> {
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

        jdbcTemplate.batchUpdate(sql, stopTimes, props.jdbcBatch().stopTimes(), (ps, st) -> {
            ps.setString(1, st.getTripId());
            ps.setString(2, st.getStopId());
            ps.setTime(3, Time.valueOf(st.getArrivalTime()));
            ps.setTime(4, Time.valueOf(st.getDepartureTime()));
            ps.setInt(5, st.getStopSequence());
        });

        log.info("Inserted stop_times={}", stopTimes.size());
    }
}
