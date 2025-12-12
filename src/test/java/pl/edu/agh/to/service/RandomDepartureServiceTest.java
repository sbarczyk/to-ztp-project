package pl.edu.agh.to.service;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.transit.realtime.GtfsRealtime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import pl.edu.agh.to.model.RandomDepartureDto;

import java.time.Instant;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class RandomDepartureServiceTest {

    private GtfsClient client;
    private GtfsParser parser;
    private Random random;
    private RandomDepartureService service;

    @BeforeEach
    void setUp() {
        client = Mockito.mock(GtfsClient.class);
        parser = Mockito.mock(GtfsParser.class);
        random = new Random(0);
        service = new RandomDepartureService(client, parser, random);
    }

    @Test
    void shouldReturnDto_givenValidData_thenCorrectDtoReturned() throws Exception {
        // given
        byte[] bytes = new byte[]{1, 2, 3};
        Mockito.when(client.fetchTripUpdatesAsBytes()).thenReturn(bytes);

        long now = Instant.now().getEpochSecond();
        GtfsRealtime.TripUpdate tripUpdate = createValidTripUpdate(now);

        Mockito.when(parser.parseTripUpdates(bytes))
                .thenReturn(List.of(tripUpdate));

        // when
        RandomDepartureDto dto = service.getRandomDepartureInfo();

        // then
        assertEquals("M:401", dto.getVehicleId());
        assertEquals("2048408", dto.getStopId());
        assertNotNull(dto.getDepartureTime());
    }

    @Test
    void shouldThrowException_givenNoTrips_thenIllegalStateThrown() throws Exception {
        // given
        Mockito.when(client.fetchTripUpdatesAsBytes()).thenReturn(new byte[]{});
        Mockito.when(parser.parseTripUpdates(Mockito.any()))
                .thenReturn(List.of());

        // when / then
        assertThrows(IllegalStateException.class,
                () -> service.getRandomDepartureInfo());
    }

    @Test
    void shouldThrowException_givenNoStops_thenIllegalStateThrown() throws Exception {
        // given
        Mockito.when(client.fetchTripUpdatesAsBytes()).thenReturn(new byte[]{});

        GtfsRealtime.TripUpdate tripUpdate =
                GtfsRealtime.TripUpdate.newBuilder()
                        .setTrip(GtfsRealtime.TripDescriptor.newBuilder().setTripId("x").build())
                        .setVehicle(GtfsRealtime.VehicleDescriptor.newBuilder().setId("M:401").build())
                        .build();

        Mockito.when(parser.parseTripUpdates(Mockito.any()))
                .thenReturn(List.of(tripUpdate));

        // when / then
        assertThrows(IllegalStateException.class,
                () -> service.getRandomDepartureInfo());
    }

    @Test
    void shouldThrowException_givenStopWithoutDepartureTime_thenIllegalStateThrown() throws Exception {
        // given
        Mockito.when(client.fetchTripUpdatesAsBytes()).thenReturn(new byte[]{});

        GtfsRealtime.TripUpdate.StopTimeUpdate stop =
                GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder()
                        .setStopId("2048408")
                        .build();

        GtfsRealtime.TripUpdate tripUpdate =
                GtfsRealtime.TripUpdate.newBuilder()
                        .setTrip(GtfsRealtime.TripDescriptor.newBuilder().setTripId("x").build())
                        .setVehicle(GtfsRealtime.VehicleDescriptor.newBuilder().setId("M:401").build())
                        .addStopTimeUpdate(stop)
                        .build();

        Mockito.when(parser.parseTripUpdates(Mockito.any()))
                .thenReturn(List.of(tripUpdate));

        // when / then
        assertThrows(IllegalStateException.class,
                () -> service.getRandomDepartureInfo());
    }

    @Test
    void shouldPropagateException_givenParserThrowsException_thenExceptionPropagated() throws Exception {
        // given
        Mockito.when(client.fetchTripUpdatesAsBytes()).thenReturn(new byte[]{});
        Mockito.when(parser.parseTripUpdates(Mockito.any()))
                .thenThrow(new InvalidProtocolBufferException("invalid"));

        // when / then
        assertThrows(InvalidProtocolBufferException.class,
                () -> service.getRandomDepartureInfo());
    }

    private GtfsRealtime.TripUpdate createValidTripUpdate(long epochTime) {

        GtfsRealtime.TripDescriptor tripDescriptor =
                GtfsRealtime.TripDescriptor.newBuilder()
                        .setTripId("test-trip")
                        .build();

        GtfsRealtime.VehicleDescriptor vehicleDescriptor =
                GtfsRealtime.VehicleDescriptor.newBuilder()
                        .setId("M:401")
                        .build();

        GtfsRealtime.TripUpdate.StopTimeUpdate stopTimeUpdate =
                GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder()
                        .setStopId("2048408")
                        .setDeparture(
                                GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder()
                                        .setTime(epochTime)
                                        .build()
                        )
                        .build();

        return GtfsRealtime.TripUpdate.newBuilder()
                .setTrip(tripDescriptor)
                .setVehicle(vehicleDescriptor)
                .addStopTimeUpdate(stopTimeUpdate)
                .build();
    }
}