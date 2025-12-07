package pl.edu.agh.to.service;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.transit.realtime.GtfsRealtime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class GTFSServiceTest {

    private byte[] fakeGtfsData;

    @BeforeEach
    void setUp() throws Exception {
        fakeGtfsData = createFakeGtfsFeed();
    }

    private GTFSService createServiceWithFakeData() {
        return new GTFSService((WebClient) null) {
            @Override
            protected byte[] getTripUpdatesAsBytes() {
                return fakeGtfsData;
            }
        };
    }

    @Test
    void shouldParseTripUpdatesCorrectly_givenValidGtfsData_whenGetTripUpdates_thenTripIsParsed() throws Exception {
        // given
        GTFSService service = createServiceWithFakeData();

        // when
        var result = service.getTripUpdates();

        // then
        assertEquals(1, result.size());
        assertEquals("M:401", result.getFirst().getVehicle().getId());
    }

    @Test
    void shouldReturnRandomDepartureInfo_givenValidGtfsData_whenGetRandomDepartureInfo_thenFormattedOutputReturned() throws Exception {
        // given
        GTFSService service = createServiceWithFakeData();

        // when
        String result = service.getRandomDepartureInfo();

        // then
        assertTrue(result.contains("Vehicle ID: M:401"));
        assertTrue(result.contains("Stop ID: 2048408"));
        assertTrue(result.contains("Departure time"));
    }

    private byte[] createFakeGtfsFeed() throws InvalidProtocolBufferException {
        long now = Instant.now().getEpochSecond();

        GtfsRealtime.TripUpdate.StopTimeUpdate stopTimeUpdate =
                GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder()
                        .setStopId("2048408")
                        .setDeparture(
                                GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder()
                                        .setTime(now)
                                        .build()
                        )
                        .build();

        GtfsRealtime.TripDescriptor tripDescriptor =
                GtfsRealtime.TripDescriptor.newBuilder()
                        .setTripId("test-trip")
                        .build();

        GtfsRealtime.VehicleDescriptor vehicleDescriptor =
                GtfsRealtime.VehicleDescriptor.newBuilder()
                        .setId("M:401")
                        .build();

        GtfsRealtime.TripUpdate tripUpdate =
                GtfsRealtime.TripUpdate.newBuilder()
                        .setTrip(tripDescriptor)
                        .setVehicle(vehicleDescriptor)
                        .addStopTimeUpdate(stopTimeUpdate)
                        .build();

        GtfsRealtime.FeedEntity entity =
                GtfsRealtime.FeedEntity.newBuilder()
                        .setId("entity-1")
                        .setTripUpdate(tripUpdate)
                        .build();

        GtfsRealtime.FeedHeader header =
                GtfsRealtime.FeedHeader.newBuilder()
                        .setGtfsRealtimeVersion("2.0")
                        .setIncrementality(GtfsRealtime.FeedHeader.Incrementality.FULL_DATASET)
                        .setTimestamp(Instant.now().getEpochSecond())
                        .build();

        GtfsRealtime.FeedMessage feedMessage =
                GtfsRealtime.FeedMessage.newBuilder()
                        .setHeader(header)
                        .addEntity(entity)
                        .build();

        return feedMessage.toByteArray();
    }
}
