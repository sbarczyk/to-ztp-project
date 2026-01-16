package pl.edu.agh.to.gtfs.statics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import pl.edu.agh.to.config.GtfsProperties;
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
    private final GtfsProperties props;

    public Instant getRemoteLastModified() {
        String file = props.statics().file();
        log.info("Checking remote Last-Modified for {}", file);

        try {
            return webClient.head()
                    .uri("/" + file)
                    .retrieve()
                    .toBodilessEntity()
                    .map(response -> {
                        long lastMod = response.getHeaders().getLastModified();
                        return lastMod > 0 ? Instant.ofEpochMilli(lastMod) : Instant.EPOCH;
                    })
                    .block();
        } catch (Exception ex) {
            throw new StaticGtfsDownloadException("Failed to fetch remote metadata for " + file, ex);
        }
    }

    public Path downloadZipToDisk(Instant remoteTimestamp) {
        ensureDataDirExists();

        String file = props.statics().file().trim();
        Path targetPath = Path.of(props.statics().dataDir(), file);

        log.info("Downloading {} -> {}", file, targetPath.toAbsolutePath());

        try {
            Files.deleteIfExists(targetPath);

            Flux<DataBuffer> body = webClient.get()
                    .uri("/" + file)
                    .retrieve()
                    .bodyToFlux(DataBuffer.class);

            try (AsynchronousFileChannel channel = AsynchronousFileChannel.open(
                    targetPath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING
            )) {
                DataBufferUtils.write(body, channel).then().block();
            }

            Files.setLastModifiedTime(targetPath, FileTime.from(remoteTimestamp));
            log.info("Download completed (timestamp={})", remoteTimestamp);
            return targetPath;

        } catch (IOException ex) {
            throw new StaticGtfsDownloadException("Failed to download GTFS zip to disk", ex);
        }
    }

    private void ensureDataDirExists() {
        try {
            Files.createDirectories(Path.of(props.statics().dataDir()));
        } catch (IOException ex) {
            throw new StaticGtfsDownloadException("Failed to create data directory", ex);
        }
    }
}