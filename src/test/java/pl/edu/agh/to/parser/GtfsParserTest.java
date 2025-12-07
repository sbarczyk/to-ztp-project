package pl.edu.agh.to.parser;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.transit.realtime.GtfsRealtime;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GtfsParserTest {

    private final GtfsParser parser = new GtfsParser();

    @Test
    void shouldParseTripUpdatesCorrectly_givenValidFeed_thenTripParsed() throws Exception {
        // given
        byte[] feed = createFeedWithTrips(1);

        // when
        List<GtfsRealtime.TripUpdate> result = parser.parseTripUpdates(feed);

        // then
        assertEquals(1, result.size());
        assertEquals("M:401", result.getFirst().getVehicle().getId());
    }


    @Test
    void shouldReturnEmptyList_givenFeedWithNoEntities_thenEmptyListReturned() throws Exception {
        // given
        GtfsRealtime.FeedHeader header =
                GtfsRealtime.FeedHeader.newBuilder()
                        .setGtfsRealtimeVersion("2.0")
                        .setIncrementality(GtfsRealtime.FeedHeader.Incrementality.FULL_DATASET)
                        .setTimestamp(Instant.now().getEpochSecond())
                        .build();

        GtfsRealtime.FeedMessage feedMessage =
                GtfsRealtime.FeedMessage.newBuilder()
                        .setHeader(header)
                        .build();

        byte[] feed = feedMessage.toByteArray();

        // when
        List<GtfsRealtime.TripUpdate> result = parser.parseTripUpdates(feed);

        // then
        assertTrue(result.isEmpty());
    }


    @Test
    void shouldIgnoreEntityWithoutTripUpdate_givenFeedWithOnlyEmptyEntity_thenEmptyListReturned() throws Exception {
        // given
        long now = Instant.now().getEpochSecond();

        GtfsRealtime.FeedEntity entity =
                GtfsRealtime.FeedEntity.newBuilder()
                        .setId("entity-1")
                        .build();

        GtfsRealtime.FeedHeader header =
                GtfsRealtime.FeedHeader.newBuilder()
                        .setGtfsRealtimeVersion("2.0")
                        .setIncrementality(GtfsRealtime.FeedHeader.Incrementality.FULL_DATASET)
                        .setTimestamp(now)
                        .build();

        GtfsRealtime.FeedMessage feedMessage =
                GtfsRealtime.FeedMessage.newBuilder()
                        .setHeader(header)
                        .addEntity(entity)
                        .build();

        byte[] feed = feedMessage.toByteArray();

        // when
        List<GtfsRealtime.TripUpdate> result = parser.parseTripUpdates(feed);

        // then
        assertTrue(result.isEmpty());
    }


    @Test
    void shouldParseAllTrips_givenFeedWithMultipleTrips_thenAllTripsReturned() throws Exception {
        // given
        byte[] feed = createFeedWithTrips(3);

        // when
        List<GtfsRealtime.TripUpdate> result = parser.parseTripUpdates(feed);

        // then
        assertEquals(3, result.size());
    }


    @Test
    void shouldThrowException_givenInvalidProtobufBytes_thenExceptionThrown() {
        // given
        byte[] invalidData = new byte[]{1, 2, 3, 4, 5};

        // when + then
        assertThrows(InvalidProtocolBufferException.class,
                () -> parser.parseTripUpdates(invalidData));
    }

    private byte[] createFeedWithTrips(int numberOfTrips) throws InvalidProtocolBufferException {
        long now = Instant.now().getEpochSecond();

        GtfsRealtime.FeedHeader header =
                GtfsRealtime.FeedHeader.newBuilder()
                        .setGtfsRealtimeVersion("2.0")
                        .setIncrementality(GtfsRealtime.FeedHeader.Incrementality.FULL_DATASET)
                        .setTimestamp(now)
                        .build();

        GtfsRealtime.FeedMessage.Builder feedBuilder =
                GtfsRealtime.FeedMessage.newBuilder()
                        .setHeader(header);

        for (int i = 0; i < numberOfTrips; i++) {
            feedBuilder.addEntity(createValidEntity(i, now));
        }

        return feedBuilder.build().toByteArray();
    }

    private GtfsRealtime.FeedEntity createValidEntity(int index, long epochTime) {

        GtfsRealtime.TripDescriptor tripDescriptor =
                GtfsRealtime.TripDescriptor.newBuilder()
                        .setTripId("trip-" + index)
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

        GtfsRealtime.TripUpdate tripUpdate =
                GtfsRealtime.TripUpdate.newBuilder()
                        .setTrip(tripDescriptor)
                        .setVehicle(vehicleDescriptor)
                        .addStopTimeUpdate(stopTimeUpdate)
                        .build();

        return GtfsRealtime.FeedEntity.newBuilder()
                .setId("entity-" + index)
                .setTripUpdate(tripUpdate)
                .build();
    }
}