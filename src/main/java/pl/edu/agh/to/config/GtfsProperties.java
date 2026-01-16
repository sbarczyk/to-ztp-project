package pl.edu.agh.to.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ztp.gtfs")
public record GtfsProperties(
        String url,
        StaticProperties statics,
        long checkIntervalMs,
        JdbcBatchProperties jdbcBatch
) {
    public record StaticProperties(
            String file,
            String dataDir,
            FilenameProperties filenames
    ) {
        public record FilenameProperties(
                String routes,
                String stops,
                String calendar,
                String calendarDates,
                String trips,
                String stopTimes
        ) { }
    }

    public record JdbcBatchProperties(
            int trips,
            int stopTimes
    ) { }
}