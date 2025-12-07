package pl.edu.agh.to.parser;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.transit.realtime.GtfsRealtime;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GtfsParser {

    public List<GtfsRealtime.TripUpdate> parseTripUpdates(byte[] data)
            throws InvalidProtocolBufferException {

        GtfsRealtime.FeedMessage feedMessage =
                GtfsRealtime.FeedMessage.parseFrom(data);

        return feedMessage.getEntityList().stream()
                .filter(GtfsRealtime.FeedEntity::hasTripUpdate)
                .map(GtfsRealtime.FeedEntity::getTripUpdate)
                .toList();
    }
}