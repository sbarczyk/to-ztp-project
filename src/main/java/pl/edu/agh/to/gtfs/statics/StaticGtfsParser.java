package pl.edu.agh.to.gtfs.statics;

import org.springframework.stereotype.Service;
import pl.edu.agh.to.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class StaticGtfsParser {

    private static final DateTimeFormatter dateFMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DayOfWeek[] daysMapping = DayOfWeek.values();

    private static final class StopsColumns {
        static final int ID = 0;
        static final int NAME = 2;
        static final int LAT = 4;
        static final int LON = 5;

        private StopsColumns() {}
    }

    private static final class TripsColumns {
        static final int ID = 2;
        static final int ROUTE_ID = 0;
        static final int SERVICE_ID = 1;

        private TripsColumns() {}
    }

    private static final class StopTimesColumns {
        static final int TRIP_ID = 0;
        static final int STOP_ID = 3;
        static final int ARRIVAL_TIME = 1;
        static final int DEPARTURE_TIME = 2;
        static final int STOP_SEQUENCE = 4;

        private StopTimesColumns() {}
    }

    private static final class CalendarDatesColumns {
        static final int SERVICE_ID = 0;
        static final int DATE = 1;
        static final int EXCEPTION_TYPE = 2;

        private CalendarDatesColumns() {}
    }

    private static final class RouteColumns {
        static final int ID = 0;
        static final int SHORT_NAME = 2;
        static final int LONG_NAME = 3;
        static final int TYPE = 4;

        private RouteColumns() {}
    }

    private static final class CalendarColumns {
        static final int SERVICE_ID = 0;
        static final int START_DATE = 8;
        static final int END_DATE = 9;

        private CalendarColumns() {}
    }

    public List<Stop> parseStops(Path path) throws IOException {
        return processFile(path)
                .map(line -> new Stop(
                        line[StopsColumns.ID],
                        line[StopsColumns.NAME],
                        Double.parseDouble(line[StopsColumns.LAT]),
                        Double.parseDouble(line[StopsColumns.LON])))
                .toList();
    }

    public List<Trip> parseTrips(Path path) throws IOException {
        return processFile(path)
                .map(p -> new Trip(
                        p[TripsColumns.ID],
                        p[TripsColumns.ROUTE_ID],
                        p[TripsColumns.SERVICE_ID]))
                .toList();
    }

    public List<StopTime> parseStopTimes(Path path) throws IOException {
        return processFile(path)
                .map(p -> new StopTime(
                        p[StopTimesColumns.TRIP_ID],
                        p[StopTimesColumns.STOP_ID],
                        parseTime(p[StopTimesColumns.ARRIVAL_TIME]),
                        parseTime(p[StopTimesColumns.DEPARTURE_TIME]),
                        Integer.parseInt(p[StopTimesColumns.STOP_SEQUENCE])))
                .toList();
    }

    public List<CalendarDate> parseCalendarDates(Path path) throws IOException {
        if (!Files.exists(path)) return List.of();
        return processFile(path)
                .map(p -> new CalendarDate(
                        p[CalendarDatesColumns.SERVICE_ID],
                        LocalDate.parse(p[CalendarDatesColumns.DATE], dateFMT),
                        Integer.parseInt(p[CalendarDatesColumns.EXCEPTION_TYPE])))
                .toList();
    }

    public List<Route> parseRoutes(Path path) throws IOException {
        return processFile(path)
                .map(p -> new Route(
                        p[RouteColumns.ID],
                        p[RouteColumns.SHORT_NAME],
                        p[RouteColumns.LONG_NAME],
                        Integer.parseInt(p[RouteColumns.TYPE])))
                .toList();
    }

    public List<Calendar> parseCalendar(Path path) throws IOException {
        if (!Files.exists(path)) return List.of();
        return processFile(path)
                .map(p -> {
                    Set<DayOfWeek> days = Arrays.stream(daysMapping)
                            .filter(d -> "1".equals(p[d.ordinal() + 1]))
                            .collect(Collectors.toSet());
                    return new Calendar(
                            p[CalendarColumns.SERVICE_ID],
                            days,
                            LocalDate.parse(p[CalendarColumns.START_DATE], dateFMT),
                            LocalDate.parse(p[CalendarColumns.END_DATE], dateFMT));
                })
                .toList();
    }

    private Stream<String[]> processFile(Path path) throws IOException {
        return Files.lines(path)
                .skip(1)
                .filter(line -> !line.isBlank())
                .map(this::parseLine);
    }

    private String[] parseLine(String line) {
        return Arrays.stream(line.split(",", -1))
                .map(String::trim)
                .map(this::stripQuotes)
                .toArray(String[]::new);
    }

    private String stripQuotes(String field) {
        if (field.length() >= 2 && field.startsWith("\"") && field.endsWith("\"")) {
            return field.substring(1, field.length() - 1);
        }
        return field;
    }

    private LocalTime parseTime(String time) {
        String[] parts = time.split(":");
        int h = Integer.parseInt(parts[0]) % 24; // LocalTime.of() requires proper ranges
        return LocalTime.of(h, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }
}