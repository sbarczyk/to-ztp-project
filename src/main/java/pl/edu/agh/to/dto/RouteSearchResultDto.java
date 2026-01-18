package pl.edu.agh.to.dto;

import lombok.Builder;
import lombok.Value;
import java.time.LocalTime;

@Builder
@Value
public class RouteSearchResultDto {
    String startStop;
    String endStop;
    String tripId;
    String routeId;
    String routeShortName;
    LocalTime departureTime;
    LocalTime arrivalTime;
    long delayInSeconds;
}