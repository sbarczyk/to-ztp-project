package pl.edu.agh.to.gtfs.statics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import pl.edu.agh.to.config.GtfsProperties;
import pl.edu.agh.to.exceptions.StaticGtfsDownloadException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaticGtfsClientTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private GtfsProperties props;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private WebClient webClient;

    @Mock
    private ResponseEntity<Void> responseEntity;

    @InjectMocks
    private StaticGtfsClient staticGtfsClient;

    @Test
    void shouldReturnRemoteTimestampWhenHeaderIsPresent() {
        // Given
        String fileName = "gtfs.zip";
        long lastModifiedMillis = 1704711812000L;
        Instant expectedInstant = Instant.ofEpochMilli(lastModifiedMillis);

        when(props.statics().file()).thenReturn(fileName);
        HttpHeaders headers = new HttpHeaders();
        headers.setLastModified(lastModifiedMillis);

        when(webClient.head().uri(anyString()).retrieve().toBodilessEntity())
                .thenReturn(Mono.just(responseEntity));
        when(responseEntity.getHeaders()).thenReturn(headers);

        // When
        Instant result = staticGtfsClient.getRemoteLastModified();

        // Then
        assertEquals(expectedInstant, result);
    }

    @Test
    void shouldDownloadZipToDiskSuccessfully() {
        // Given
        String fileName = "gtfs.zip";
        String dataDir = "data";
        Instant timestamp = Instant.now();

        when(props.statics().file()).thenReturn(fileName);
        when(props.statics().dataDir()).thenReturn(dataDir);

        DataBuffer mockBuffer = mock(DataBuffer.class);
        when(webClient.get().uri(anyString()).retrieve().bodyToFlux(DataBuffer.class))
                .thenReturn(Flux.just(mockBuffer));

        try (MockedStatic<Path> pathMock = mockStatic(Path.class);
             MockedStatic<Files> filesMock = mockStatic(Files.class);
             MockedStatic<AsynchronousFileChannel> channelMock = mockStatic(AsynchronousFileChannel.class);
             MockedStatic<DataBufferUtils> dbUtilsMock = mockStatic(DataBufferUtils.class)) {

            Path mockDirPath = mock(Path.class);
            Path mockFilePath = mock(Path.class);

            pathMock.when(() -> Path.of(dataDir)).thenReturn(mockDirPath);
            pathMock.when(() -> Path.of(dataDir, fileName)).thenReturn(mockFilePath);
            when(mockFilePath.toAbsolutePath()).thenReturn(mockFilePath);

            AsynchronousFileChannel mockChannel = mock(AsynchronousFileChannel.class);
            channelMock.when(() -> AsynchronousFileChannel.open(eq(mockFilePath), any(OpenOption.class), any(OpenOption.class), any(OpenOption.class)))
                    .thenReturn(mockChannel);

            dbUtilsMock.when(() -> DataBufferUtils.write(any(Flux.class), eq(mockChannel)))
                    .thenReturn(Flux.empty());

            // When
            Path result = staticGtfsClient.downloadZipToDisk(timestamp);

            // Then
            assertEquals(mockFilePath, result);
            filesMock.verify(() -> Files.createDirectories(mockDirPath));
            filesMock.verify(() -> Files.deleteIfExists(mockFilePath));
        }
    }

    @Test
    void shouldThrowStaticGtfsDownloadExceptionOnIoErrorDuringDirCreation() {
        // Given
        String dataDir = "data";
        when(props.statics().dataDir()).thenReturn(dataDir);

        try (MockedStatic<Path> pathMock = mockStatic(Path.class);
             MockedStatic<Files> filesMock = mockStatic(Files.class)) {

            Path mockPath = mock(Path.class);
            pathMock.when(() -> Path.of(dataDir)).thenReturn(mockPath);
            filesMock.when(() -> Files.createDirectories(mockPath)).thenThrow(new IOException("Disk Full"));

            // When
            Executable action = () -> staticGtfsClient.downloadZipToDisk(Instant.now());

            // Then
            assertThrows(StaticGtfsDownloadException.class, action);
        }
    }

    @Test
    void shouldThrowStaticGtfsDownloadExceptionWhenRemoteCheckFails() {
        // Given
        when(props.statics().file()).thenReturn("file.zip");
        when(webClient.head().uri(anyString())).thenThrow(new RuntimeException("Network error"));

        // When
        Executable action = () -> staticGtfsClient.getRemoteLastModified();

        // Then
        assertThrows(StaticGtfsDownloadException.class, action);
    }
}