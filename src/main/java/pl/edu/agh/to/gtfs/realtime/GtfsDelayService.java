package pl.edu.agh.to.gtfs.realtime;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.transit.realtime.GtfsRealtime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GtfsDelayService {

    private final GtfsClient gtfsClient;
    private final GtfsParser gtfsParser;

    /**
     * Fetches real-time trip updates and maps tripId to delay in seconds.
     */
    public Map<String, Long> getCurrentDelays() {
        Map<String, Long> delays = new HashMap<>();
        try {
            byte[] data = gtfsClient.fetchTripUpdatesAsBytes();
            List<GtfsRealtime.TripUpdate> updates = gtfsParser.parseTripUpdates(data);

            for (GtfsRealtime.TripUpdate tu : updates) {
                if (tu.hasTrip() && tu.getStopTimeUpdateCount() > 0) {
                    String tripId = tu.getTrip().getTripId();
                    long delay = tu.getStopTimeUpdate(0).getArrival().getDelay();
                    delays.put(tripId, delay);
                }
            }
        } catch (InvalidProtocolBufferException e) {
            log.error("Failed to parse GTFS Realtime data: {}", e.getMessage());
        }
        return delays;
    }
}