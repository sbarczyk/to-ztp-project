package pl.edu.agh.to.service;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.transit.realtime.GtfsRealtime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.edu.agh.to.exceptions.NotFoundException;
import pl.edu.agh.to.gtfs.realtime.GtfsClient;
import pl.edu.agh.to.gtfs.realtime.GtfsParser;
import pl.edu.agh.to.model.RandomDepartureDto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Random;

/**
 * Returns a random departure from realtime feed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RandomDepartureService {

    private static final int MAX_ATTEMPTS = 20;

    private final GtfsClient gtfsClient;
    private final GtfsParser gtfsParser;
    private final Random random;

    public RandomDepartureDto getRandomDepartureInfo() throws InvalidProtocolBufferException {
        byte[] data = gtfsClient.fetchTripUpdatesAsBytes();
        List<GtfsRealtime.TripUpdate> trips = gtfsParser.parseTripUpdates(data);

        if (trips.isEmpty()) {
            throw new NotFoundException("No trip updates available");
        }

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            GtfsRealtime.TripUpdate randomTrip = trips.get(random.nextInt(trips.size()));
            List<GtfsRealtime.TripUpdate.StopTimeUpdate> stops = randomTrip.getStopTimeUpdateList();
            if (stops.isEmpty()) {
                continue;
            }

            GtfsRealtime.TripUpdate.StopTimeUpdate randomStop = stops.get(random.nextInt(stops.size()));
            if (!randomStop.hasDeparture() || !randomStop.getDeparture().hasTime()) {
                continue;
            }

            if (!randomTrip.hasVehicle() || randomTrip.getVehicle().getId().isBlank()) {
                continue;
            }

            String stopId = randomStop.getStopId();
            if (stopId == null || stopId.isBlank()) {
                continue;
            }

            long departureEpoch = randomStop.getDeparture().getTime();
            LocalDateTime departureTime = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(departureEpoch),
                    ZoneId.systemDefault()
            );

            log.debug("Random departure selected in {} attempt(s)", attempt);

            return RandomDepartureDto.builder()
                    .vehicleId(randomTrip.getVehicle().getId())
                    .stopId(stopId)
                    .departureTime(departureTime)
                    .build();
        }

        throw new NotFoundException("Unable to find valid random departure after " + MAX_ATTEMPTS + " attempts");
    }
}