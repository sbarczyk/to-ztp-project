package pl.edu.agh.to.service;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.transit.realtime.GtfsRealtime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.edu.agh.to.exceptions.NotFoundException;
import pl.edu.agh.to.gtfs.realtime.GtfsClient;
import pl.edu.agh.to.dto.RandomDepartureDto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.IntStream;


@Slf4j
@Service
@RequiredArgsConstructor
public class RandomDepartureService {

    private static final int MAX_ATTEMPTS = 20;

    private final GtfsClient gtfsClient;
    private final Random random;

    public RandomDepartureDto getRandomDepartureInfo() throws InvalidProtocolBufferException {
        List<GtfsRealtime.TripUpdate> trips = gtfsClient.fetchTripUpdates();

        if (trips.isEmpty()) {
            throw new NotFoundException("No trip updates available");
        }

        return IntStream.range(0, MAX_ATTEMPTS)
                .mapToObj(i -> selectRandomValidDeparture(trips))
                .flatMap(Optional::stream)
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Unable to find valid random departure after " + MAX_ATTEMPTS + " attempts"));
    }

    private Optional<RandomDepartureDto> selectRandomValidDeparture(List<GtfsRealtime.TripUpdate> trips) {
        GtfsRealtime.TripUpdate randomTrip = trips.get(random.nextInt(trips.size()));

        if (!randomTrip.hasVehicle() || randomTrip.getVehicle().getId().isBlank()) {
            return Optional.empty();
        }

        List<GtfsRealtime.TripUpdate.StopTimeUpdate> stops = randomTrip.getStopTimeUpdateList();
        if (stops.isEmpty()) {
            return Optional.empty();
        }

        GtfsRealtime.TripUpdate.StopTimeUpdate randomStop = stops.get(random.nextInt(stops.size()));

        if (!isValidStop(randomStop)) {
            return Optional.empty();
        }

        return Optional.of(mapToDto(randomTrip, randomStop));
    }

    private boolean isValidStop(GtfsRealtime.TripUpdate.StopTimeUpdate stop) {
        return stop.hasDeparture()
                && stop.getDeparture().hasTime()
                && stop.getStopId() != null
                && !stop.getStopId().isBlank();
    }

    private RandomDepartureDto mapToDto(GtfsRealtime.TripUpdate trip, GtfsRealtime.TripUpdate.StopTimeUpdate stop) {
        long departureEpoch = stop.getDeparture().getTime();
        LocalDateTime departureTime = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(departureEpoch),
                ZoneId.systemDefault()
        );

        return RandomDepartureDto.builder()
                .vehicleId(trip.getVehicle().getId())
                .stopId(stop.getStopId())
                .departureTime(departureTime)
                .build();
    }
}