package pl.edu.agh.to.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;

@Entity
@Table(name = "stop_times", indexes = {
        @Index(name = "idx_stop_id", columnList = "stopId"),
        @Index(name = "idx_trip_id", columnList = "tripId")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class StopTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Baza sama nada numer ID

    private String tripId;
    private String stopId;

    private LocalTime arrivalTime;
    private LocalTime departureTime;
    private int stopSequence;

    // Konstruktor, którego używasz w parserze
    public StopTime(String tripId, String stopId, LocalTime arrivalTime, LocalTime departureTime, int stopSequence) {
        this.tripId = tripId;
        this.stopId = stopId;
        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
        this.stopSequence = stopSequence;
    }
}