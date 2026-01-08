package pl.edu.agh.to.gtfs.realtime;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.transit.realtime.GtfsRealtime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
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
     * Fetches current delays. If the real-time API is unavailable or corrupted,
     * returns an empty map, allowing the system to fall back to static schedules.
     */
    public Map<String, Long> getCurrentDelays() {
        try {
            byte[] data = gtfsClient.fetchTripUpdatesAsBytes();
            List<GtfsRealtime.TripUpdate> updates = gtfsParser.parseTripUpdates(data);
            return processUpdates(updates);
        } catch (InvalidProtocolBufferException e) {
            log.warn("GTFS Realtime parsing failed (Protobuf error). Falling back to zero delays. Details: {}", e.getMessage());
            return Collections.emptyMap();
        } catch (Exception e) {
            log.error("Unexpected error fetching real-time data. Falling back to zero delays.", e);
            return Collections.emptyMap();
        }
    }

    private Map<String, Long> processUpdates(List<GtfsRealtime.TripUpdate> updates) {
        Map<String, Long> delays = new HashMap<>();
        for (GtfsRealtime.TripUpdate tu : updates) {
            if (tu.hasTrip() && tu.getStopTimeUpdateCount() > 0) {
                String tripId = tu.getTrip().getTripId();
                delays.put(tripId, extractDelayValue(tu));
            }
        }
        return delays;
    }

    private long extractDelayValue(GtfsRealtime.TripUpdate tu) {
        var firstUpdate = tu.getStopTimeUpdate(0);
        if (firstUpdate.hasArrival()) {
            return firstUpdate.getArrival().getDelay();
        } else if (firstUpdate.hasDeparture()) {
            return firstUpdate.getDeparture().getDelay();
        }
        return 0L;
    }
}