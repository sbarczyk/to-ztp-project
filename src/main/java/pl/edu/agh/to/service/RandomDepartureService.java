package pl.edu.agh.to.service;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.transit.realtime.GtfsRealtime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.edu.agh.to.model.RandomDepartureDto;


import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

/**
 * Service responsible for fetching, parsing, and processing GTFS data
 * to return a randomly selected departure.
 */
@Service
@RequiredArgsConstructor
public class RandomDepartureService {

    private final GtfsClient gtfsClient;
    private final GtfsParser gtfsParser;
    private final Random random;

    /**
     * Fetches GTFS data, selects a random trip update, and extracts departure information.
     *
     * @return RandomDepartureDto containing the processed information.
     * @throws InvalidProtocolBufferException if Protobuf data is invalid.
     * @throws IllegalStateException if critical data (trips, stops, or time) is missing.
     */
    public RandomDepartureDto getRandomDepartureInfo() throws InvalidProtocolBufferException {

        byte[] data = gtfsClient.fetchTripUpdatesAsBytes();

        List<GtfsRealtime.TripUpdate> trips = gtfsParser.parseTripUpdates(data);

        if (trips.isEmpty()) {
            throw new NoSuchElementException("No trip updates available");
        }

        GtfsRealtime.TripUpdate randomTrip =
                trips.get(random.nextInt(trips.size()));

        List<GtfsRealtime.TripUpdate.StopTimeUpdate> stops =
                randomTrip.getStopTimeUpdateList();

        if (stops.isEmpty()) {
            throw new NoSuchElementException("No stop updates for selected trip");
        }

        GtfsRealtime.TripUpdate.StopTimeUpdate randomStop =
                stops.get(random.nextInt(stops.size()));

        if (!randomStop.hasDeparture() || !randomStop.getDeparture().hasTime()) {
            throw new NoSuchElementException("No departure time available");
        }


        String stopId = randomStop.getStopId();
        long departureEpoch = randomStop.getDeparture().getTime();

        LocalDateTime departureTime = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(departureEpoch),
                ZoneId.systemDefault()
        );

        return RandomDepartureDto.builder()
                .vehicleId(randomTrip.getVehicle().getId())
                .stopId(stopId)
                .departureTime(departureTime)
                .build();
    }
}