package pl.edu.agh.to.gtfs.realtime;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.transit.realtime.GtfsRealtime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes delays (seconds) per tripId from realtime feed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GtfsDelayService {

    private final GtfsClient gtfsClient;
    private final GtfsParser gtfsParser;

    public Map<String, Long> getCurrentDelays() throws InvalidProtocolBufferException {
        byte[] data = gtfsClient.fetchTripUpdatesAsBytes();
        List<GtfsRealtime.TripUpdate> updates = gtfsParser.parseTripUpdates(data);

        Map<String, Long> delays = new HashMap<>();
        for (GtfsRealtime.TripUpdate tu : updates) {
            if (!tu.hasTrip() || tu.getStopTimeUpdateCount() == 0) {
                continue;
            }

            String tripId = tu.getTrip().getTripId();
            long delay = 0L;

            GtfsRealtime.TripUpdate.StopTimeUpdate first = tu.getStopTimeUpdate(0);
            if (first.hasArrival() && first.getArrival().hasDelay()) {
                delay = first.getArrival().getDelay();
            }

            delays.put(tripId, delay);
        }

        log.debug("Computed delays for {} trips", delays.size());
        return delays;
    }
}