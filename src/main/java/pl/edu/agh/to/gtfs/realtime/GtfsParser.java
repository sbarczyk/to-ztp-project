package pl.edu.agh.to.gtfs.realtime;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.transit.realtime.GtfsRealtime;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Parses GTFS Realtime protobuf payload into domain structures.
 */
@Service
public class GtfsParser {

    public List<GtfsRealtime.TripUpdate> parseTripUpdates(byte[] data) throws InvalidProtocolBufferException {
        GtfsRealtime.FeedMessage feedMessage = GtfsRealtime.FeedMessage.parseFrom(data);

        return feedMessage.getEntityList().stream()
                .filter(GtfsRealtime.FeedEntity::hasTripUpdate)
                .map(GtfsRealtime.FeedEntity::getTripUpdate)
                .toList();
    }
}