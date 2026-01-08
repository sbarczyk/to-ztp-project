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

    // Mocki zależności "logicznych" (serwisy, repozytoria, baza)
    @Mock private StaticGtfsClient staticGtfsClient;
    @Mock private GtfsZipExtractor zipExtractor;
    @Mock private StaticGtfsParser parser;
    @Mock private JdbcTemplate jdbcTemplate;

    @Mock private RouteRepository routeRepository;
    @Mock private StopRepository stopRepository;
    @Mock private CalendarRepository calendarRepository;
    @Mock private CalendarDateRepository calendarDateRepository;

    // Testowany serwis (nie używamy @InjectMocks, tworzymy go ręcznie)
    private StaticGtfsService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // 1. Tworzymy PRAWDZIWY obiekt konfiguracyjny (Record)
        // Struktura: GtfsProperties(url, StaticProperties(file, dataDir), checkInterval)
        GtfsProperties.StaticProperties staticProps = new GtfsProperties.StaticProperties(
                "GTFS_KRK.zip",
                tempDir.toString()
        );

        GtfsProperties realProps = new GtfsProperties(
                "http://fake.url", // URL nie ma znaczenia w tym teście
                staticProps,
                3600000L
        );

        // 2. Ręcznie tworzymy serwis, wstrzykując mocki i prawdziwy config
        // Kolejność argumentów musi zgadzać się z polami w StaticGtfsService (Lombok)
        service = new StaticGtfsService(
                staticGtfsClient,
                zipExtractor,
                parser,
                jdbcTemplate,
                realProps, // <-- Tu wchodzi nasz prawdziwy rekord
                routeRepository,
                stopRepository,
                calendarRepository,
                calendarDateRepository
        );

        // 3. Ustawiamy wartości @Value przy użyciu refleksji
        ReflectionTestUtils.setField(service, "TRIPS_BATCH_SIZE", 100);
        ReflectionTestUtils.setField(service, "STOP_TIMES_BATCH_SIZE", 100);
    }

    @Test
    void shouldRefreshData_WhenBazaIsEmpty() throws Exception { // Dodaj throws Exception
        // Given
        Instant remoteTime = Instant.now();
        when(staticGtfsClient.getRemoteLastModified()).thenReturn(remoteTime);

        // --- POPRAWKA: Tworzymy pusty plik lokalny ---
        // Dzięki temu Files.exists() zwróci true, a kod przejdzie do sprawdzania bazy danych (isStopsTableEmpty)
        Path localZip = tempDir.resolve("GTFS_KRK.zip");
        Files.createFile(localZip);
        // ---------------------------------------------

        // Symulacja: Baza jest pusta
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

        // Tworzymy fizyczny plik lokalny
        Path localZip = tempDir.resolve("GTFS_KRK.zip");
        Files.createFile(localZip);
        Files.setLastModifiedTime(localZip, java.nio.file.attribute.FileTime.from(localTime));

        when(staticGtfsClient.getRemoteLastModified()).thenReturn(remoteTime);
        // Baza ma dane
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

        // Tworzymy plik, by wymusić import
        Path localZip = tempDir.resolve("GTFS_KRK.zip");
        Files.createFile(localZip);

        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM stops"), eq(Integer.class))).thenReturn(0);
        when(staticGtfsClient.downloadZipToDisk(remoteTime)).thenReturn(tempDir.resolve("new.zip"));

        // --- TU JEST ZMIANA: Mockujemy zwracanie danych dla Trip i StopTime ---
        // Dzięki temu test wejdzie do metod batchInsertTrips i batchInsertStopTimes

        // 1. Mockowanie Tras (Trips)
        Trip mockTrip = new Trip("trip1", "route1", "service1");
        List<Trip> trips = List.of(mockTrip);
        when(parser.parseTrips(any())).thenReturn(trips);

        // 2. Mockowanie Czasów (StopTimes)
        StopTime mockStopTime = new StopTime("trip1", "stop1", java.time.LocalTime.NOON, java.time.LocalTime.NOON, 1);
        List<StopTime> stopTimes = List.of(mockStopTime);
        when(parser.parseStopTimes(any())).thenReturn(stopTimes);

        // 3. Mockowanie reszty (Routes, Stops...)
        when(parser.parseRoutes(any())).thenReturn(List.of(new Route("r1", "1", "Bus", 3)));
        when(parser.parseStops(any())).thenReturn(List.of(new Stop("s1", "Stop1", 50.0, 19.0)));

        // When
        service.refreshDataIfNeeded();

        // Then
        // Weryfikacja repozytoriów JPA
        verify(routeRepository).saveAll(anyList());
        verify(stopRepository).saveAll(anyList());

        // --- WERYFIKACJA JDBC (To pokrywa batchInsertTrips i batchInsertStopTimes) ---
        // Sprawdzamy, czy wywołano batchUpdate dla tabeli trips
        verify(jdbcTemplate).batchUpdate(
                contains("INSERT INTO trips"), // Sprawdzamy fragment SQL
                eq(trips),                     // Czy przekazano naszą listę
                eq(100),                       // Czy batchSize się zgadza (ustawiony w setUp)
                any()                          // Ignorujemy lambdę settera
        );

        // Sprawdzamy, czy wywołano batchUpdate dla tabeli stop_times
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
        // Zakładamy, że update nie jest potrzebny, żeby test był szybszy (kończy się na if)
        // Musimy stworzyć plik i ustawić, że jest aktualny
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
        // Sprawdzamy, czy metoda schedulera faktycznie odpytała klienta o czas
        verify(staticGtfsClient).getRemoteLastModified();
    }

    @Test
    void shouldTriggerRefresh_WhenInitIsCalled() {
        // Given
        Instant remoteTime = Instant.now();
        when(staticGtfsClient.getRemoteLastModified()).thenReturn(remoteTime);
        // Analogicznie, zakładamy brak potrzeby update'u
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
        // Given - wymuszenie importu
        Instant remoteTime = Instant.now();
        when(staticGtfsClient.getRemoteLastModified()).thenReturn(remoteTime);
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM stops"), eq(Integer.class))).thenReturn(0);
        when(staticGtfsClient.downloadZipToDisk(remoteTime)).thenReturn(tempDir.resolve("new.zip"));

        Path localZip = tempDir.resolve("GTFS_KRK.zip");
        Files.createFile(localZip);

        // Parser zwraca puste listy (domyślne zachowanie mocka, ale dla jasności):
        when(parser.parseTrips(any())).thenReturn(List.of());
        when(parser.parseStopTimes(any())).thenReturn(List.of());
        when(parser.parseRoutes(any())).thenReturn(List.of()); // Żeby nie rzuciło null pointera w saveIfNotEmpty

        // When
        service.refreshDataIfNeeded();

        // Then
        // NIE powinno być wywołania batchUpdate, bo listy są puste
        verify(jdbcTemplate, never()).batchUpdate(contains("INSERT INTO trips"), anyList(), anyInt(), any());
        verify(jdbcTemplate, never()).batchUpdate(contains("INSERT INTO stop_times"), anyList(), anyInt(), any());
    }
}