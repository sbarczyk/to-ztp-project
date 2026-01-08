package pl.edu.agh.to.gtfs.statics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import pl.edu.agh.to.config.GtfsProperties;
import pl.edu.agh.to.model.Route;
import pl.edu.agh.to.model.Stop;
import pl.edu.agh.to.model.StopTime;
import pl.edu.agh.to.model.Trip;
import pl.edu.agh.to.repository.CalendarDateRepository;
import pl.edu.agh.to.repository.CalendarRepository;
import pl.edu.agh.to.repository.RouteRepository;
import pl.edu.agh.to.repository.StopRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaticGtfsServiceTest {

    @Mock private StaticGtfsClient staticGtfsClient;
    @Mock private GtfsZipExtractor zipExtractor;
    @Mock private StaticGtfsParser parser;
    @Mock private JdbcTemplate jdbcTemplate;

    @Mock private RouteRepository routeRepository;
    @Mock private StopRepository stopRepository;
    @Mock private CalendarRepository calendarRepository;
    @Mock private CalendarDateRepository calendarDateRepository;

    private StaticGtfsService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        GtfsProperties.StaticProperties staticProps = new GtfsProperties.StaticProperties(
                "GTFS_KRK.zip",
                tempDir.toString()
        );

        GtfsProperties realProps = new GtfsProperties(
                "http://fake.url",
                staticProps,
                3600000L
        );

        service = new StaticGtfsService(
                staticGtfsClient,
                zipExtractor,
                parser,
                jdbcTemplate,
                realProps,
                routeRepository,
                stopRepository,
                calendarRepository,
                calendarDateRepository
        );

        ReflectionTestUtils.setField(service, "TRIPS_BATCH_SIZE", 100);
        ReflectionTestUtils.setField(service, "STOP_TIMES_BATCH_SIZE", 100);
    }

    @Test
    void shouldRefreshData_WhenBazaIsEmpty() throws Exception { // Dodaj throws Exception
        // Given
        Instant remoteTime = Instant.now();
        when(staticGtfsClient.getRemoteLastModified()).thenReturn(remoteTime);

        Path localZip = tempDir.resolve("GTFS_KRK.zip");
        Files.createFile(localZip);

        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM stops"), eq(Integer.class))).thenReturn(0);

        Path downloadedZip = tempDir.resolve("downloaded.zip");
        when(staticGtfsClient.downloadZipToDisk(remoteTime)).thenReturn(downloadedZip);

        // When
        service.refreshDataIfNeeded();

        // Then
        verify(staticGtfsClient).downloadZipToDisk(remoteTime);
        verify(zipExtractor).extractZipToDirectory(eq(downloadedZip), any(Path.class));
        verify(jdbcTemplate).update("DELETE FROM stops");
    }

    @Test
    void shouldNotRefreshData_WhenLocalFileIsUpToDate() throws Exception {
        // Given
        Instant remoteTime = Instant.parse("2025-01-01T10:00:00Z");
        Instant localTime = Instant.parse("2025-01-01T12:00:00Z"); // Lokalny plik nowszy

        Path localZip = tempDir.resolve("GTFS_KRK.zip");
        Files.createFile(localZip);
        Files.setLastModifiedTime(localZip, java.nio.file.attribute.FileTime.from(localTime));

        when(staticGtfsClient.getRemoteLastModified()).thenReturn(remoteTime);

        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM stops"), eq(Integer.class))).thenReturn(500);

        // When
        service.refreshDataIfNeeded();

        // Then
        verify(staticGtfsClient, never()).downloadZipToDisk(any());
    }

    @Test
    void shouldImportDataSuccessfully() throws Exception {
        // Given
        Instant remoteTime = Instant.now();
        when(staticGtfsClient.getRemoteLastModified()).thenReturn(remoteTime);

        Path localZip = tempDir.resolve("GTFS_KRK.zip");
        Files.createFile(localZip);

        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM stops"), eq(Integer.class))).thenReturn(0);
        when(staticGtfsClient.downloadZipToDisk(remoteTime)).thenReturn(tempDir.resolve("new.zip"));

        Trip mockTrip = new Trip("trip1", "route1", "service1");
        List<Trip> trips = List.of(mockTrip);
        when(parser.parseTrips(any())).thenReturn(trips);

        StopTime mockStopTime = new StopTime("trip1", "stop1", java.time.LocalTime.NOON, java.time.LocalTime.NOON, 1);
        List<StopTime> stopTimes = List.of(mockStopTime);
        when(parser.parseStopTimes(any())).thenReturn(stopTimes);

        when(parser.parseRoutes(any())).thenReturn(List.of(new Route("r1", "1", "Bus", 3)));
        when(parser.parseStops(any())).thenReturn(List.of(new Stop("s1", "Stop1", 50.0, 19.0)));

        // When
        service.refreshDataIfNeeded();

        // Then
        verify(routeRepository).saveAll(anyList());
        verify(stopRepository).saveAll(anyList());

        verify(jdbcTemplate).batchUpdate(
                contains("INSERT INTO trips"),
                eq(trips),
                eq(100),
                any()
        );

        verify(jdbcTemplate).batchUpdate(
                contains("INSERT INTO stop_times"),
                eq(stopTimes),
                eq(100),
                any()
        );
    }

    @Test
    void shouldTriggerRefresh_WhenScheduledRefreshIsCalled() {
        // Given
        Instant remoteTime = Instant.now();
        when(staticGtfsClient.getRemoteLastModified()).thenReturn(remoteTime);
        try {
            Path localZip = tempDir.resolve("GTFS_KRK.zip");
            if (!Files.exists(localZip)) {
                Files.createFile(localZip);
                Files.setLastModifiedTime(localZip, java.nio.file.attribute.FileTime.from(remoteTime.plusSeconds(3600)));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // When
        service.scheduledRefresh();

        // Then
        verify(staticGtfsClient).getRemoteLastModified();
    }

    @Test
    void shouldTriggerRefresh_WhenInitIsCalled() {
        // Given
        Instant remoteTime = Instant.now();
        when(staticGtfsClient.getRemoteLastModified()).thenReturn(remoteTime);
        try {
            Path localZip = tempDir.resolve("GTFS_KRK.zip");
            if (!Files.exists(localZip)) {
                Files.createFile(localZip);
                Files.setLastModifiedTime(localZip, java.nio.file.attribute.FileTime.from(remoteTime.plusSeconds(3600)));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // When
        service.init();

        // Then
        verify(staticGtfsClient).getRemoteLastModified();
    }

    @Test
    void shouldSkipBatchInsert_WhenParserReturnsEmptyLists() throws Exception {
        // Given
        Instant remoteTime = Instant.now();
        when(staticGtfsClient.getRemoteLastModified()).thenReturn(remoteTime);
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM stops"), eq(Integer.class))).thenReturn(0);
        when(staticGtfsClient.downloadZipToDisk(remoteTime)).thenReturn(tempDir.resolve("new.zip"));

        Path localZip = tempDir.resolve("GTFS_KRK.zip");
        Files.createFile(localZip);


        when(parser.parseTrips(any())).thenReturn(List.of());
        when(parser.parseStopTimes(any())).thenReturn(List.of());
        when(parser.parseRoutes(any())).thenReturn(List.of());

        // When
        service.refreshDataIfNeeded();

        // Then
        verify(jdbcTemplate, never()).batchUpdate(contains("INSERT INTO trips"), anyList(), anyInt(), any());
        verify(jdbcTemplate, never()).batchUpdate(contains("INSERT INTO stop_times"), anyList(), anyInt(), any());
    }
}