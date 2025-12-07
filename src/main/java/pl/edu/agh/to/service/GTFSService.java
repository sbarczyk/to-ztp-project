package pl.edu.agh.to.service;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.transit.realtime.GtfsRealtime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
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
    private byte[] getTripUpdatesAsBytes() {
        log.info("Fetching TripUpdates.pb from GTFS Realtime API...");
        return webClient.get()
                .uri("/TripUpdates.pb")
                .retrieve()
                .bodyToMono(byte[].class)
                .block();
    }

    /**
     * Fetches and parses trip updates from GTFS Realtime API.
     * @return List<GtfsRealtime.TripUpdate> - GTFS trip updates.
     */
    public List<GtfsRealtime.TripUpdate> getTripUpdates() throws InvalidProtocolBufferException {
        byte[] dataBytes = getTripUpdatesAsBytes();
        GtfsRealtime.FeedMessage feedMessage = GtfsRealtime.FeedMessage.parseFrom(dataBytes);
        return feedMessage.getEntityList().stream()
                .filter(GtfsRealtime.FeedEntity::hasTripUpdate)
                .map(GtfsRealtime.FeedEntity::getTripUpdate)
                .toList();
    }

    // Żeby zobaczyć czy działa
    // todo: po zrobieniu unit testa należy wywalić
    public String getTestTripString() {
        try {
            return getTripUpdates().getFirst().toString();
        } catch (InvalidProtocolBufferException e) {
            return e.getMessage();
        }
    }
}