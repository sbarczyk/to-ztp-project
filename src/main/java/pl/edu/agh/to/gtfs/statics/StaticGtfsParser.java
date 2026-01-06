package pl.edu.agh.to.gtfs.statics;

import org.springframework.stereotype.Service;
import pl.edu.agh.to.model.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class StaticGtfsParser {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public List<Stop> parseStops(Path path) throws IOException {
        List<Stop> list = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(path)) {
            br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = parseLine(line);

                list.add(new Stop(p[0], p[2], Double.parseDouble(p[4]), Double.parseDouble(p[5])));
            }
        }
        return list;
    }


    public List<Trip> parseTrips(Path path) throws IOException {
        List<Trip> list = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(path)) {
            // 1. Odczytaj nagłówek, żeby znaleźć indeksy kolumn
            String headerLine = br.readLine();
            if (headerLine == null) return list;

            if (headerLine.startsWith("\uFEFF")) {
                headerLine = headerLine.substring(1);
            }

            String[] headers = parseLine(headerLine);
            int tripIdx = -1;
            int routeIdx = -1;
            int serviceIdx = -1;

            // Szukamy, na której pozycji są poszczególne kolumny
            for (int i = 0; i < headers.length; i++) {
                if (headers[i].equalsIgnoreCase("trip_id")) tripIdx = i;
                if (headers[i].equalsIgnoreCase("route_id")) routeIdx = i;
                if (headers[i].equalsIgnoreCase("service_id")) serviceIdx = i;
            }

            // Zabezpieczenie, gdyby plik był uszkodzony
            if (tripIdx == -1 || routeIdx == -1 || serviceIdx == -1) {
                throw new IOException("Brak wymaganych kolumn w pliku: " + path);
            }

            // 2. Czytamy dane używając wykrytych indeksów
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = parseLine(line);

                // Konstruktor Trip(tripId, routeId, serviceId)
                // Pobieramy dane z dynamicznie znalezionych kolumn
                list.add(new Trip(
                        p[tripIdx],   // Tu zawsze trafi trip_id, niezależnie gdzie jest w pliku
                        p[routeIdx],  // Tu route_id
                        p[serviceIdx] // Tu service_id
                ));
            }
        }
        return list;
    }

    public List<StopTime> parseStopTimes(Path path) throws IOException {
        List<StopTime> list = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(path)) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {

                String[] p = parseLine(line);
                list.add(new StopTime(p[0], p[3], parseTime(p[1]), parseTime(p[2]), Integer.parseInt(p[4])));
            }
        }
        return list;
    }

    public List<CalendarDate> parseCalendarDates(Path path) throws IOException {
        List<CalendarDate> list = new ArrayList<>();
        if (!Files.exists(path)) return list;
        try (BufferedReader br = Files.newBufferedReader(path)) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {

                String[] p = parseLine(line);


//                System.out.println("Loaded Exception: ServiceID='" + p[0] + "', Data=" + p[1]);
                // calendar_dates.txt: service_id(0), date(1), exception_type(2)
                list.add(new CalendarDate(
                        p[0],
                        LocalDate.parse(p[1], DATE_FMT),
                        Integer.parseInt(p[2])
                ));
            }
        }
        return list;
    }



    private String[] parseLine(String line) {
        String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

        for (int i = 0; i < parts.length; i++) {
            String clean = parts[i].trim();

            parts[i] = stripQuotes(clean);
        }
        return parts;
    }

    private String stripQuotes(String field) {
        if (field.startsWith("\"") && field.endsWith("\"")) {
            return field.substring(1, field.length() - 1);
        }
        return field;
    }

    private LocalTime parseTime(String time) {
        String[] parts = time.split(":");
        int h = Integer.parseInt(parts[0]);
        if (h >= 24) h -= 24;
        return LocalTime.of(h, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }

    public List<Calendar> parseCalendar(Path path) throws IOException {
        List<Calendar> list = new ArrayList<>();
        if (!Files.exists(path)) return list;

        try (BufferedReader br = Files.newBufferedReader(path)) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = parseLine(line);

                Set<DayOfWeek> days = new HashSet<>();
                if (p[1].equals("1")) days.add(DayOfWeek.MONDAY);
                if (p[2].equals("1")) days.add(DayOfWeek.TUESDAY);
                if (p[3].equals("1")) days.add(DayOfWeek.WEDNESDAY);
                if (p[4].equals("1")) days.add(DayOfWeek.THURSDAY);
                if (p[5].equals("1")) days.add(DayOfWeek.FRIDAY);
                if (p[6].equals("1")) days.add(DayOfWeek.SATURDAY);
                if (p[7].equals("1")) days.add(DayOfWeek.SUNDAY);

                LocalDate start = LocalDate.parse(p[8], DATE_FMT);
                LocalDate end = LocalDate.parse(p[9], DATE_FMT);

                list.add(new Calendar(p[0], days, start, end));
            }
        }
        return list;
    }
}