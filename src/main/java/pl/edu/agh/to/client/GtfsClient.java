package pl.edu.agh.to.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Client for fetching GTFS Realtime data from the external ZTP API.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GtfsClient {

    private final WebClient webClient;

    /**
     * Fetches binary trip update data from the GTFS API endpoint.
     * @return Raw byte array containing GTFS Realtime Protobuf data.
     */
    public byte[] fetchTripUpdatesAsBytes() {
        log.info("Fetching TripUpdates.pb from GTFS API");
        return webClient.get()
                .uri("/TripUpdates.pb")
                .retrieve()
                .bodyToMono(byte[].class)
                .block(); // Blocking call is acceptable here for simplicity
    }
}