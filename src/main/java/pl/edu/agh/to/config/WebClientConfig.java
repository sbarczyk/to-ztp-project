package pl.edu.agh.to.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    /**
     * Creates and registers a WebClient bean.
     * @return WebClient - Configured WebClient for the GTFS Realtime API
     */
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl("https://gtfs.ztp.krakow.pl")
                .build();
    }
}