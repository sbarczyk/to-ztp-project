package pl.edu.agh.to.gtfs.realtime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import pl.edu.agh.to.exceptions.ExternalServiceException;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class GtfsClient {

    private final WebClient webClient;

    public byte[] fetchTripUpdatesAsBytes() {
        log.debug("Fetching GTFS Realtime TripUpdates.pb");
        try {
            byte[] body = webClient.get()
                    .uri("/TripUpdates.pb")
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();

            return Objects.requireNonNull(body, "TripUpdates.pb payload is null");
        } catch (Exception ex) {
            throw new ExternalServiceException("Failed to fetch TripUpdates.pb from external GTFS endpoint", ex);
        }
    }
}