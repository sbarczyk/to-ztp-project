package pl.edu.agh.to.service;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.transit.realtime.GtfsRealtime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import lombok.RequiredArgsConstructor;
import pl.edu.agh.to.config.WebClientConfig;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Random;

/**
  Service to fetch GTFS Realtime trip updates as a byte array.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GTFSService {

    private final WebClient webClient;
    private final Random random = new Random();

    /**
     * Fetches trip updates as bytes from GTFS Realtime API.
     * @return byte[] - GTFS trip updates in byte format.
     */
    protected byte[] getTripUpdatesAsBytes() {
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

    public String getRandomDepartureInfo() throws InvalidProtocolBufferException {

        List<GtfsRealtime.TripUpdate> trips = getTripUpdates();

        GtfsRealtime.TripUpdate randomTrip =
                trips.get(random.nextInt(trips.size()));

        List<GtfsRealtime.TripUpdate.StopTimeUpdate> stops =
                randomTrip.getStopTimeUpdateList();

        GtfsRealtime.TripUpdate.StopTimeUpdate randomStop =
                stops.get(random.nextInt(stops.size()));

        String stopId = randomStop.getStopId();

        long departureEpoch = randomStop.getDeparture().getTime();

        LocalDateTime departureTime = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(departureEpoch),
                ZoneId.systemDefault()
        );

        return """
                Vehicle ID: %s
                Stop ID: %s
                Departure time: %s
                """.formatted(
                randomTrip.getVehicle().getId(),
                stopId,
                departureTime
        );
    }
}