package pl.edu.agh.to.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class GtfsClient {

    private final WebClient webClient;

    public byte[] fetchTripUpdatesAsBytes() {
        log.info("Fetching TripUpdates.pb from GTFS API");
        return webClient.get()
                .uri("/TripUpdates.pb")
                .retrieve()
                .bodyToMono(byte[].class)
                .block();
    }
}