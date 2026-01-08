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

    public List<Stop> parseStops(Path path) throws IOException {
        return processFile(path)
                .map(p -> new Stop(p[0], p[2], Double.parseDouble(p[4]), Double.parseDouble(p[5])))
                .toList();
    }

    public List<Trip> parseTrips(Path path) throws IOException {
        return processFile(path)
                .map(p -> new Trip(p[2], p[0], p[1]))
                .toList();
    }

    public List<StopTime> parseStopTimes(Path path) throws IOException {
        return processFile(path)
                .map(p -> new StopTime(p[0], p[3], parseTime(p[1]), parseTime(p[2]), Integer.parseInt(p[4])))
                .toList();
    }

    public List<CalendarDate> parseCalendarDates(Path path) throws IOException {
        if (!Files.exists(path)) return List.of();
        return processFile(path)
                .map(p -> new CalendarDate(p[0], LocalDate.parse(p[1], dateFMT), Integer.parseInt(p[2])))
                .toList();
    }

    public List<Route> parseRoutes(Path path) throws IOException {
        return processFile(path)
                .map(p -> new Route(p[0], p[2], p[3], Integer.parseInt(p[4])))
                .toList();
    }

    public List<Calendar> parseCalendar(Path path) throws IOException {
        if (!Files.exists(path)) return List.of();
        return processFile(path)
                .map(p -> {
                    Set<DayOfWeek> days = Arrays.stream(daysMapping)
                            .filter(d -> "1".equals(p[d.ordinal() + 1]))
                            .collect(Collectors.toSet());
                    return new Calendar(p[0], days, LocalDate.parse(p[8], dateFMT), LocalDate.parse(p[9], dateFMT));
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
        int h = Integer.parseInt(parts[0]);
        return LocalTime.of(h % 24, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }
}