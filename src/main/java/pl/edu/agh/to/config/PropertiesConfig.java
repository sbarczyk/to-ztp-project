package pl.edu.agh.to.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GtfsProperties.class)
public class PropertiesConfig {
}