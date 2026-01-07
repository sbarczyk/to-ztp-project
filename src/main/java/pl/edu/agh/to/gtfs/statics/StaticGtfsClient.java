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

    public Path downloadZipToDisk() {
        ensureDataDirExists();

        String fileName = staticFile.trim();
        Path targetPath = Path.of(dataDir, fileName);

        downloadSingleZipToDisk(fileName, targetPath);
        log.info("Saved {} to {}", fileName, targetPath.toAbsolutePath());

        return targetPath;
    }

    private void downloadSingleZipToDisk(String fileName, Path targetPath) {
        String url = baseUrl + "/" + fileName;
        log.info("Downloading GTFS Static zip from {}", url);

        try {
            Files.deleteIfExists(targetPath);
        } catch (IOException e) {
            throw new StaticGtfsDownloadException("Failed to delete existing file: " + targetPath, e);
        }

        Flux<DataBuffer> body = webClient.get()
                .uri(url)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(msg -> new StaticGtfsDownloadException(
                                        "Failed to download " + fileName + " (HTTP " + response.statusCode() + ") " + msg,
                                        null
                                ))
                )
                .bodyToFlux(DataBuffer.class);

        try (AsynchronousFileChannel channel = AsynchronousFileChannel.open(
                targetPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
        )) {
            DataBufferUtils.write(body, channel)
                    .doOnError(ex -> log.error("Failed to download {}", fileName, ex))
                    .then()
                    .block();
        } catch (IOException e) {
            throw new StaticGtfsDownloadException("Failed to open file channel for: " + targetPath, e);
        } catch (RuntimeException e) {
            throw new StaticGtfsDownloadException("Failed to download file: " + fileName, e);
        }
    }

    private void ensureDataDirExists() {
        try {
            Files.createDirectories(Path.of(dataDir));
        } catch (IOException e) {
            throw new StaticGtfsDownloadException("Failed to create data directory: " + dataDir, e);
        }
    }
}