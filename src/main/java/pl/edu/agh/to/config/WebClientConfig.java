package pl.edu.agh.to.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for the WebClient used to connect to the external GTFS service.
 */
@Configuration
public class WebClientConfig {

    @Value("${ztp.gtfs.url}")
    private String gtfsUrl;
    private final int maxMemorySize = 10 * 1024 * 1024; // 10 MB


    @Bean
    public WebClient webClient() {
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(maxMemorySize))
                .build();

        return WebClient.builder()
                .exchangeStrategies(exchangeStrategies)
                .baseUrl(gtfsUrl)
                .build();
    }
}