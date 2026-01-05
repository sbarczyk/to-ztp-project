package pl.edu.agh.to.model;

import lombok.Value;
import java.time.LocalTime;

@Value
public class StopTime {
    String tripId;
    String stopId;
    LocalTime arrivalTime;
    LocalTime departureTime;
    int stopSequence;
}