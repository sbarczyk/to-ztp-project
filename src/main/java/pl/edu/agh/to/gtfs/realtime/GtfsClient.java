package pl.edu.agh.to.gtfs.realtime;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.transit.realtime.GtfsRealtime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import pl.edu.agh.to.exceptions.ExternalServiceException;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class GtfsClient {

    private final WebClient webClient;
    private final GtfsParser gtfsParser;

    public List<GtfsRealtime.TripUpdate> fetchTripUpdates() throws InvalidProtocolBufferException {
        log.debug("Fetching GTFS Realtime TripUpdates.pb");
        try {
            byte[] body = webClient.get()
                    .uri("/TripUpdates.pb")
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();

            Objects.requireNonNull(body, "TripUpdates.pb payload is null");
            return gtfsParser.parseTripUpdates(body);
        } catch (Exception ex) {
            throw new ExternalServiceException("Failed to fetch TripUpdates.pb from external GTFS endpoint", ex);
        }
    }
}