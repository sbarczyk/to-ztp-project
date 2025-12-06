package pl.edu.agh.to.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    private final int maxMemorySize = 1024 * 1024; // 1 MB
    private final ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(maxMemorySize))
            .build();

    /**
     * Creates and registers a WebClient bean.
     * @return WebClient - Configured WebClient for the GTFS Realtime API
     */
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .exchangeStrategies(exchangeStrategies)
                .baseUrl("https://gtfs.ztp.krakow.pl")
                .build();
    }
}