package pl.edu.agh.to.gtfs.realtime;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.transit.realtime.GtfsRealtime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GtfsDelayServiceTest {

    @Mock
    private GtfsClient gtfsClient;

    @Mock
    private GtfsParser gtfsParser;

    @InjectMocks
    private GtfsDelayService gtfsDelayService;

    @Test
    void shouldReturnEmptyMapWhenParserThrowsInvalidProtocolBufferException() throws InvalidProtocolBufferException {
        // Given
        byte[] dummyData = new byte[]{1, 2, 3};
        when(gtfsClient.fetchTripUpdatesAsBytes()).thenReturn(dummyData);
        when(gtfsParser.parseTripUpdates(dummyData)).thenThrow(new InvalidProtocolBufferException("Parse error"));

        // When
        Map<String, Long> result = gtfsDelayService.getCurrentDelays();

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyMapWhenGenericExceptionOccurs() {
        // Given
        when(gtfsClient.fetchTripUpdatesAsBytes()).thenThrow(new RuntimeException("Network error"));

        // When
        Map<String, Long> result = gtfsDelayService.getCurrentDelays();

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldExtractArrivalDelayCorrectly() throws InvalidProtocolBufferException {
        // Given
        String tripId = "T1";
        long delayValue = 150L;
        GtfsRealtime.TripUpdate tu = createTripUpdate(tripId, delayValue, true);

        when(gtfsClient.fetchTripUpdatesAsBytes()).thenReturn(new byte[0]);
        when(gtfsParser.parseTripUpdates(any())).thenReturn(List.of(tu));

        // When
        Map<String, Long> result = gtfsDelayService.getCurrentDelays();

        // Then
        assertEquals(1, result.size());
        assertEquals(delayValue, result.get(tripId));
    }

    @Test
    void shouldExtractDepartureDelayWhenArrivalIsMissing() throws InvalidProtocolBufferException {
        // Given
        String tripId = "T2";
        long delayValue = 200L;
        GtfsRealtime.TripUpdate tu = createTripUpdate(tripId, delayValue, false);

        when(gtfsClient.fetchTripUpdatesAsBytes()).thenReturn(new byte[0]);
        when(gtfsParser.parseTripUpdates(any())).thenReturn(List.of(tu));

        // When
        Map<String, Long> result = gtfsDelayService.getCurrentDelays();

        // Then
        assertEquals(1, result.size());
        assertEquals(delayValue, result.get(tripId));
    }

    @Test
    void shouldReturnZeroDelayWhenNoArrivalNorDepartureTimeIsPresent() throws InvalidProtocolBufferException {
        // Given
        String tripId = "T3";
        GtfsRealtime.TripUpdate.StopTimeUpdate stu = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder()
                .setStopSequence(1)
                .build();

        GtfsRealtime.TripUpdate tu = GtfsRealtime.TripUpdate.newBuilder()
                .setTrip(GtfsRealtime.TripDescriptor.newBuilder().setTripId(tripId).build())
                .addStopTimeUpdate(stu)
                .build();

        when(gtfsClient.fetchTripUpdatesAsBytes()).thenReturn(new byte[0]);
        when(gtfsParser.parseTripUpdates(any())).thenReturn(List.of(tu));

        // When
        Map<String, Long> result = gtfsDelayService.getCurrentDelays();

        // Then
        assertEquals(1, result.size());
        assertEquals(0L, result.get(tripId));
    }

    @Test
    void shouldSkipUpdateWhenTripDescriptorIsMissing() throws InvalidProtocolBufferException {
        // Given
        GtfsRealtime.TripUpdate tu = GtfsRealtime.TripUpdate.newBuilder()
                .addStopTimeUpdate(GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder().setStopSequence(1).build())
                .buildPartial();

        when(gtfsClient.fetchTripUpdatesAsBytes()).thenReturn(new byte[0]);
        when(gtfsParser.parseTripUpdates(any())).thenReturn(List.of(tu));

        // When
        Map<String, Long> result = gtfsDelayService.getCurrentDelays();

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldSkipUpdateWhenStopTimeUpdatesAreEmpty() throws InvalidProtocolBufferException {
        // Given
        GtfsRealtime.TripUpdate tu = GtfsRealtime.TripUpdate.newBuilder()
                .setTrip(GtfsRealtime.TripDescriptor.newBuilder().setTripId("T4").build())
                .build();

        when(gtfsClient.fetchTripUpdatesAsBytes()).thenReturn(new byte[0]);
        when(gtfsParser.parseTripUpdates(any())).thenReturn(List.of(tu));

        // When
        Map<String, Long> result = gtfsDelayService.getCurrentDelays();

        // Then
        assertTrue(result.isEmpty());
    }

    private GtfsRealtime.TripUpdate createTripUpdate(String tripId, long delay, boolean isArrival) {
        GtfsRealtime.TripUpdate.StopTimeUpdate.Builder stuBuilder =
                GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder().setStopSequence(1);

        if (isArrival) {
            stuBuilder.setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay((int) delay).build());
        } else {
            stuBuilder.setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay((int) delay).build());
        }

        return GtfsRealtime.TripUpdate.newBuilder()
                .setTrip(GtfsRealtime.TripDescriptor.newBuilder().setTripId(tripId).build())
                .addStopTimeUpdate(stuBuilder.build())
                .build();
    }
}