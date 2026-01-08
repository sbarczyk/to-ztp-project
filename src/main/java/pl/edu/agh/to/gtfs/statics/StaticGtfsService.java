package pl.edu.agh.to.gtfs.statics;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.to.config.GtfsProperties;
import pl.edu.agh.to.model.Calendar;
import pl.edu.agh.to.model.CalendarDate;
import pl.edu.agh.to.model.Route;
import pl.edu.agh.to.model.Stop;
import pl.edu.agh.to.model.StopTime;
import pl.edu.agh.to.model.Trip;
import pl.edu.agh.to.repository.CalendarDateRepository;
import pl.edu.agh.to.repository.CalendarRepository;
import pl.edu.agh.to.repository.RouteRepository;
import pl.edu.agh.to.repository.StopRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Time;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

/**
 * Periodically downloads and imports GTFS Static feed into the database.
 * <p>
 * Small tables are persisted through JPA repositories for readability,
 * while large tables (trips, stop_times) are inserted with JdbcTemplate for performance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StaticGtfsService {

    private final StaticGtfsClient staticGtfsClient;
    private final GtfsZipExtractor zipExtractor;
    private final JdbcTemplate jdbcTemplate;
    private final GtfsProperties props;
    private final StaticGtfsImporter staticGtfsImporter;

    @PostConstruct
    public void init() {
        refreshDataIfNeeded();
    }

    @Scheduled(fixedRateString = "${ztp.gtfs.check-interval-ms:3600000}")
    public void scheduledRefresh() {
        log.info("Scheduled GTFS Static check started");
        refreshDataIfNeeded();
    }

    public synchronized void refreshDataIfNeeded() {
        Instant remoteTime = staticGtfsClient.getRemoteLastModified();
        Path localZip = Path.of(props.statics().dataDir(), props.statics().file());

        if (!isUpdateNeeded(localZip, remoteTime)) {
            log.info("GTFS Static is up to date (remote={})", remoteTime);
            return;
        }

        log.info("GTFS Static update needed (remote={})", remoteTime);
        performReload(remoteTime);
    }

    private boolean isUpdateNeeded(Path localZip, Instant remoteTime) {
        try {
            if (!Files.exists(localZip) || isStopsTableEmpty()) {
                return true;
            }
            Instant localTime = Files.getLastModifiedTime(localZip).toInstant();
            return remoteTime.isAfter(localTime);
        } catch (IOException ex) {
            log.warn("Failed to read local GTFS timestamp, forcing update: {}", ex.getMessage());
            return true;
        }
    }

    private boolean isStopsTableEmpty() {
        Integer cnt = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stops", Integer.class);
        return cnt == null || cnt == 0;
    }

    private void performReload(Instant remoteTime) {
        try {
            Path zipPath = staticGtfsClient.downloadZipToDisk(remoteTime);

            String datasetName = props.statics().file().replace(".zip", "");
            Path targetDir = Path.of(props.statics().dataDir(), "extracted", datasetName);

            log.info("Extracting GTFS zip to {}", targetDir.toAbsolutePath());
            zipExtractor.extractZipToDirectory(zipPath, targetDir);

            staticGtfsImporter.importToDatabase(targetDir);

            log.info("GTFS Static reload finished successfully");
        } catch (Exception ex) {
            log.error("Failed to reload GTFS Static data: {}", ex.getMessage(), ex);
        }
    }
}