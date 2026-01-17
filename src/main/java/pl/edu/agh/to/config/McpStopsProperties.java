package pl.edu.agh.to.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "ztp.mcp.stops")
public record McpStopsProperties(
        @Min(1) @Max(1000) int defaultSize,
        @Min(1) @Max(5000) int maxSize
) { }