package pl.edu.agh.to.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ztp.gtfs")
public record GtfsProperties(
        String url,
        StaticProperties statics,
        long checkIntervalMs
) {
    public record StaticProperties(
            String file,
            String dataDir
    ) { }
}