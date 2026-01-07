package pl.edu.agh.to.gtfs.statics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import pl.edu.agh.to.exceptions.StaticGtfsDownloadException;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaticGtfsClient {

    private final WebClient webClient;

    @Value("${ztp.gtfs.url}")
    private String baseUrl;

    @Value("${ztp.gtfs.static.file}")
    private String staticFile;

    @Value("${ztp.gtfs.data-dir}")
    private String dataDir;

    public Instant getRemoteLastModified() {
        log.info("Checking remote metadata for: {}/{}", baseUrl, staticFile);
        try {
            return webClient.head()
                    .uri("/" + staticFile)
                    .retrieve()
                    .toBodilessEntity()
                    .map(response -> {
                        long lastMod = response.getHeaders().getLastModified();
                        return lastMod != -1 ? Instant.ofEpochMilli(lastMod) : Instant.now();
                    })
                    .block();
        } catch (Exception e) {
            log.error("Failed to fetch remote metadata: {}", e.getMessage());
            return Instant.MIN;
        }
    }

    public Path downloadZipToDisk(Instant remoteTimestamp) {
        ensureDataDirExists();
        Path targetPath = Path.of(dataDir, staticFile.trim());
        log.info("Starting download of {} to {}", staticFile, targetPath.toAbsolutePath());

        try {
            Files.deleteIfExists(targetPath);
            Flux<DataBuffer> body = webClient.get()
                    .uri("/" + staticFile.trim())
                    .retrieve()
                    .bodyToFlux(DataBuffer.class);

            try (AsynchronousFileChannel channel = AsynchronousFileChannel.open(
                    targetPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                DataBufferUtils.write(body, channel).then().block();
            }

            Files.setLastModifiedTime(targetPath, FileTime.from(remoteTimestamp));
            log.info("Download completed and timestamp synced: {}", remoteTimestamp);

        } catch (IOException e) {
            throw new StaticGtfsDownloadException("Download or sync failed", e);
        }
        return targetPath;
    }

    private void ensureDataDirExists() {
        try {
            Files.createDirectories(Path.of(dataDir));
        } catch (IOException e) {
            throw new StaticGtfsDownloadException("Directory error", e);
        }
    }
}