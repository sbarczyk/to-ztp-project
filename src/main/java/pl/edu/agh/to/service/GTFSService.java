package pl.edu.agh.to.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import lombok.RequiredArgsConstructor;

/*
  Service to fetch GTFS Realtime trip updates as a byte array.
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class GTFSService {

    private final WebClient webClient;

    /**
     * Fetches trip updates as bytes from GTFS Realtime API.
     * @return byte[] - GTFS trip updates in byte format.
     */
    public byte[] getTripUpdatesAsBytes() {
        log.info("Fetching TripUpdates.pb from GTFS Realtime API...");
        return webClient.get()
                .uri("/TripUpdates.pb")
                .retrieve()
                .bodyToMono(byte[].class)
                .block();
    }
}