package pl.edu.agh.to.gtfs.statics;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pl.edu.agh.to.exceptions.StaticGtfsExtractException;
import pl.edu.agh.to.model.CalendarDate;
import pl.edu.agh.to.model.Stop;
import pl.edu.agh.to.model.StopTime;
import pl.edu.agh.to.model.Trip;
import pl.edu.agh.to.model.Calendar;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StaticGtfsService {

    private final StaticGtfsClient staticGtfsClient;
    private final GtfsZipExtractor zipExtractor;

    @Value("#{'${ztp.gtfs.static.files}'.split(',')}")
    private List<String> staticFiles;

    @Value("${ztp.gtfs.data-dir:data}")
    private String dataDir;

    private final StaticGtfsParser parser;

    public Map<String, List<Stop>> stopsByName = new HashMap<>();
    public Map<String, Trip> tripsById = new HashMap<>();
    public Map<String, List<StopTime>> stopTimesByTripId = new HashMap<>();
    public Map<String, List<CalendarDate>> calendarDatesByServiceId = new HashMap<>();
    public Map<String, Calendar> calendarsByServiceId = new HashMap<>();

    public Map<String, Path> downloadAndExtractAll() {
        Map<String, Path> downloaded = staticGtfsClient.downloadAllZipsToDisk();

        Map<String, Path> extractedRoots = new LinkedHashMap<>();
        Path extractedBase = Path.of(dataDir, "extracted");

        for (String rawName : staticFiles) {
            String fileName = rawName.trim();

            Path zipPath = downloaded.getOrDefault(fileName, Path.of(dataDir, fileName));
            if (!Files.exists(zipPath)) {
                throw new StaticGtfsExtractException("Zip file not found: " + zipPath);
            }

            String datasetKey = datasetKeyFromFileName(fileName);
            Path targetDir = extractedBase.resolve(datasetKey);

            try {
                zipExtractor.extractZipToDirectory(zipPath, targetDir);
            } catch (RuntimeException e) {
                throw new StaticGtfsExtractException(
                        "Failed to extract dataset " + datasetKey + " from " + zipPath,
                        e
                );
            }

            extractedRoots.put(datasetKey, targetDir);
        }

        return extractedRoots;
    }

    private static String datasetKeyFromFileName(String fileName) {
        int underscore = fileName.lastIndexOf('_');
        int dot = fileName.lastIndexOf('.');
        if (underscore < 0 || dot < 0 || underscore + 1 >= dot) {
            throw new StaticGtfsExtractException("Unexpected static GTFS file name: " + fileName);
        }
        return fileName.substring(underscore + 1, dot);
    }



    @PostConstruct
    public void init() {
        Map<String, Path> paths = downloadAndExtractAll();

        for (Path root : paths.values()) {
            try {
                System.out.println("Loading data from directory: " + root);

                List<Stop> stops = parser.parseStops(root.resolve("stops.txt"));
                stops.forEach(s ->
                        stopsByName.computeIfAbsent(s.getName(), k -> new ArrayList<>()).add(s)
                );


                System.out.println("Loaded " + stopsByName.size() + " stops.");
                if (stopsByName.size() > 0) {
                    System.out.println("Example keys in the map: " +
                            stopsByName.keySet().stream().limit(3).toList());
                }

                List<Trip> trips = parser.parseTrips(root.resolve("trips.txt"));
                trips.forEach(t -> tripsById.put(t.getTripId(), t));

                List<StopTime> stopTimes = parser.parseStopTimes(root.resolve("stop_times.txt"));
                stopTimes.forEach(st ->
                        stopTimesByTripId.computeIfAbsent(st.getTripId(), k -> new ArrayList<>()).add(st)
                );

                Path calendarPath = root.resolve("calendar.txt");
                if (Files.exists(calendarPath)) {
                    List<pl.edu.agh.to.model.Calendar> calendars = parser.parseCalendar(calendarPath);
                    calendars.forEach(c -> calendarsByServiceId.put(c.getServiceId(), c));
                }

                Path calendarDatesPath = root.resolve("calendar_dates.txt");
                if (Files.exists(calendarDatesPath)) {
                    List<CalendarDate> dates = parser.parseCalendarDates(calendarDatesPath);
                    dates.forEach(cd ->
                            calendarDatesByServiceId.computeIfAbsent(cd.getServiceId(), k -> new ArrayList<>()).add(cd)
                    );
                }

            } catch (IOException e) {
                System.err.println("Error loading data " + root + ": " + e.getMessage());
            }
        }

        System.out.println("Finished loading data.");
    }
}