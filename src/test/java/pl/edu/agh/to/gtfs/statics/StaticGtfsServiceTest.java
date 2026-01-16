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
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
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
        GtfsProperties realProps = getGtfsTestProperties();

        StaticGtfsImporter importer = new StaticGtfsImporter(
                parser,
                jdbcTemplate,
                realProps,
                routeRepository,
                stopRepository,
                calendarRepository,
                calendarDateRepository
        );

        service = new StaticGtfsService(
                staticGtfsClient,
                zipExtractor,
                jdbcTemplate,
                realProps,
                importer
        );
    }

    private GtfsProperties getGtfsTestProperties() {
        GtfsProperties.StaticProperties staticProps = new GtfsProperties.StaticProperties(
                "GTFS_KRK.zip",
                tempDir.toString(),
                new GtfsProperties.StaticProperties.FilenameProperties(
                        "routes.txt", "stops.txt", "calendar.txt",
                        "calendar_dates.txt", "trips.txt", "stop_times.txt"
                )
        );

        return new GtfsProperties(
                "http://fake.url",
                staticProps,
                3600000L,
                new GtfsProperties.JdbcBatchProperties(
                        100, 100
                )
        );
    }

    @Test
    void shouldRefreshData_WhenBazaIsEmpty() throws Exception {
        // Given
        Instant remoteTime = Instant.now();
        when(staticGtfsClient.getRemoteLastModified()).thenReturn(remoteTime);

        Path localZip = tempDir.resolve("GTFS_KRK.zip");
        Files.createFile(localZip);

        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stops", Integer.class)).thenReturn(0);

        Path downloadedZip = tempDir.resolve("downloaded.zip");
        when(staticGtfsClient.downloadZipToDisk(remoteTime)).thenReturn(downloadedZip);

        // When
        service.refreshDataIfNeeded();

        // Then
        verify(staticGtfsClient).downloadZipToDisk(remoteTime);
        verify(zipExtractor).extractZipToDirectory(any(), any());
        verify(jdbcTemplate).update("DELETE FROM stops");
    }

    @Test
    void shouldNotRefreshData_WhenLocalFileIsUpToDate() throws Exception {
        // Given
        Instant remoteTime = Instant.parse("2026-01-01T10:00:00Z");
        Instant localTime = Instant.parse("2026-01-01T12:00:00Z");

        Path localZip = tempDir.resolve("GTFS_KRK.zip");
        Files.createFile(localZip);
        Files.setLastModifiedTime(localZip, java.nio.file.attribute.FileTime.from(localTime));

        when(staticGtfsClient.getRemoteLastModified()).thenReturn(remoteTime);
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stops", Integer.class)).thenReturn(500);

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

        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stops", Integer.class)).thenReturn(0);
        when(staticGtfsClient.downloadZipToDisk(remoteTime)).thenReturn(tempDir.resolve("new.zip"));

        List<Trip> trips = List.of(new Trip("trip1", "route1", "service1"));
        when(parser.parseTrips(any())).thenReturn(trips);

        List<StopTime> stopTimes = List.of(new StopTime("trip1", "stop1", LocalTime.NOON, LocalTime.NOON, 1));
        when(parser.parseStopTimes(any())).thenReturn(stopTimes);

        when(parser.parseRoutes(any())).thenReturn(List.of(new Route("r1", "1", "Bus", 3)));
        when(parser.parseStops(any())).thenReturn(List.of(new Stop("s1", "Stop1", 50.0, 19.0)));

        // When
        service.refreshDataIfNeeded();

        // Then
        verify(routeRepository).saveAll(anyList());
        verify(stopRepository).saveAll(anyList());

        verify(jdbcTemplate).batchUpdate(
                argThat(sql -> sql.contains("INSERT INTO trips")),
                anyList(),
                anyInt(),
                any()
        );

        verify(jdbcTemplate).batchUpdate(
                argThat(sql -> sql.contains("INSERT INTO stop_times")),
                anyList(),
                anyInt(),
                any()
        );
    }

    @Test
    void shouldTriggerRefresh_WhenScheduledRefreshIsCalled() {
        // Given
        Instant remoteTime = Instant.now();
        when(staticGtfsClient.getRemoteLastModified()).thenReturn(remoteTime);

        // When
        service.scheduledRefresh();

        // Then
        verify(staticGtfsClient).getRemoteLastModified();
    }

    @Test
    void shouldSkipBatchInsert_WhenParserReturnsEmptyLists() throws Exception {
        // Given
        Instant remoteTime = Instant.now();
        when(staticGtfsClient.getRemoteLastModified()).thenReturn(remoteTime);
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stops", Integer.class)).thenReturn(0);
        when(staticGtfsClient.downloadZipToDisk(remoteTime)).thenReturn(tempDir.resolve("new.zip"));

        Path localZip = tempDir.resolve("GTFS_KRK.zip");
        Files.createFile(localZip);

        when(parser.parseTrips(any())).thenReturn(List.of());
        when(parser.parseStopTimes(any())).thenReturn(List.of());
        when(parser.parseRoutes(any())).thenReturn(List.of());

        // When
        service.refreshDataIfNeeded();

        // Then
        verify(jdbcTemplate, never()).batchUpdate(anyString(), anyList(), anyInt(), any());
    }
}