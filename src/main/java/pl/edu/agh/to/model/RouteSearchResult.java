package pl.edu.agh.to.model;

import lombok.Builder;
import lombok.Value;
import java.time.LocalTime;

@Builder
@Value
public class RouteSearchResult {
    String startStop;
    String endStop;
    String tripId;
    String routeId;
    LocalTime departureTime;
    LocalTime arrivalTime;
    long delayInSeconds;
}