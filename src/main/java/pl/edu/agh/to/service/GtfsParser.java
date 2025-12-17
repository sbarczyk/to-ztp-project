package pl.edu.agh.to.service;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.transit.realtime.GtfsRealtime;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Responsible for parsing raw GTFS Realtime Protobuf data.
 */
@Service
public class GtfsParser {

    /**
     * Parses the raw byte array into a list of GtfsRealtime.TripUpdate objects.
     * Filters out entities that are not TripUpdates.
     *
     * @param data Raw Protobuf byte array from the client.
     * @return A list of valid TripUpdate entities.
     * @throws InvalidProtocolBufferException if the byte data is corrupted or invalid.
     */
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