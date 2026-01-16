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
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class GtfsDelayService {

    private final GtfsClient gtfsClient;
    private final GtfsParser gtfsParser;

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
        return updates.stream()
                .filter(GtfsRealtime.TripUpdate::hasTrip)
                .filter(update -> update.getStopTimeUpdateCount() > 0)
                .collect(Collectors.toMap(
                        update -> update.getTrip().getTripId(),
                        this::extractDelayValue
                ));
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