package pl.edu.agh.to.service;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.transit.realtime.GtfsRealtime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import pl.edu.agh.to.model.RandomDepartureDto;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RandomDepartureServiceTest {

    private GtfsClient client;
    private GtfsParser parser;
    private RandomDepartureService service;
    private static final String VEHICLE_TEST_ID = "M:401";
    private static final String STOP_TEST_ID = "2048408";
    private static final String TRIP_TEST_ID = "x";

    @BeforeEach
    void setUp() {
        client = mock(GtfsClient.class);
        parser = mock(GtfsParser.class);
        service = new RandomDepartureService(client, parser, new Random(0));
    }

    @Test
    void shouldReturnDto_givenValidData_thenCorrectDtoReturned() throws Exception {
        // given
        byte[] bytes = new byte[]{1, 2, 3};
        when(client.fetchTripUpdatesAsBytes()).thenReturn(bytes);

        long now = Instant.now().getEpochSecond();
        GtfsRealtime.TripUpdate tripUpdate = createValidTripUpdate(now);

        when(parser.parseTripUpdates(bytes))
                .thenReturn(List.of(tripUpdate));

        // when
        RandomDepartureDto dto = service.getRandomDepartureInfo();

        // then
        assertEquals(VEHICLE_TEST_ID, dto.getVehicleId());
        assertEquals(STOP_TEST_ID, dto.getStopId());
        assertNotNull(dto.getDepartureTime());
    }

    @Test
    void shouldThrowException_givenNoTrips_thenNoSuchElementThrown() throws Exception {
        // given
        when(client.fetchTripUpdatesAsBytes()).thenReturn(new byte[]{});
        when(parser.parseTripUpdates(Mockito.any()))
                .thenReturn(List.of());

        // when
        Throwable thrown = catchThrowable(() -> service.getRandomDepartureInfo());

        // then
        assertThat(thrown).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void shouldThrowException_givenNoStops_thenNoSuchElementThrown() throws Exception {
        // given
        when(client.fetchTripUpdatesAsBytes()).thenReturn(new byte[]{});

        GtfsRealtime.TripUpdate tripUpdate =
                GtfsRealtime.TripUpdate.newBuilder()
                        .setTrip(GtfsRealtime.TripDescriptor.newBuilder().setTripId(TRIP_TEST_ID).build())
                        .setVehicle(GtfsRealtime.VehicleDescriptor.newBuilder().setId(VEHICLE_TEST_ID).build())
                        .build();

        when(parser.parseTripUpdates(Mockito.any()))
                .thenReturn(List.of(tripUpdate));

        // when
        Throwable thrown = catchThrowable(() -> service.getRandomDepartureInfo());

        // then
        assertThat(thrown).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void shouldThrowException_givenStopWithoutDepartureTime_thenNoSuchElementThrown() throws Exception {
        // given
        when(client.fetchTripUpdatesAsBytes()).thenReturn(new byte[]{});

        GtfsRealtime.TripUpdate.StopTimeUpdate stop =
                GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder()
                        .setStopId(STOP_TEST_ID)
                        .build();

        GtfsRealtime.TripUpdate tripUpdate =
                GtfsRealtime.TripUpdate.newBuilder()
                        .setTrip(GtfsRealtime.TripDescriptor.newBuilder().setTripId(TRIP_TEST_ID).build())
                        .setVehicle(GtfsRealtime.VehicleDescriptor.newBuilder().setId(VEHICLE_TEST_ID).build())
                        .addStopTimeUpdate(stop)
                        .build();

        when(parser.parseTripUpdates(Mockito.any()))
                .thenReturn(List.of(tripUpdate));

        // when
        Throwable thrown = catchThrowable(() -> service.getRandomDepartureInfo());

        // then
        assertThat(thrown).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void shouldPropagateException_givenParserThrowsException_thenExceptionPropagated() throws Exception {
        // given
        when(client.fetchTripUpdatesAsBytes()).thenReturn(new byte[]{});
        when(parser.parseTripUpdates(Mockito.any()))
                .thenThrow(new InvalidProtocolBufferException("invalid"));

        // when
        Throwable thrown = catchThrowable(() -> service.getRandomDepartureInfo());

        // then
        assertThat(thrown).isInstanceOf(InvalidProtocolBufferException.class);
    }

    private GtfsRealtime.TripUpdate createValidTripUpdate(long epochTime) {

        GtfsRealtime.TripDescriptor tripDescriptor =
                GtfsRealtime.TripDescriptor.newBuilder()
                        .setTripId("test-trip")
                        .build();

        GtfsRealtime.VehicleDescriptor vehicleDescriptor =
                GtfsRealtime.VehicleDescriptor.newBuilder()
                        .setId(VEHICLE_TEST_ID)
                        .build();

        GtfsRealtime.TripUpdate.StopTimeUpdate stopTimeUpdate =
                GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder()
                        .setStopId(STOP_TEST_ID)
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