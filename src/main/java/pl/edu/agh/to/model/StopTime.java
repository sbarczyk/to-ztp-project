package pl.edu.agh.to.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(
        name = "stop_times",
        indexes = {
                @Index(name = "idx_stop_times_stop_id", columnList = "stop_id"),
                @Index(name = "idx_stop_times_trip_id", columnList = "trip_id"),
                @Index(name = "idx_stop_times_trip_seq", columnList = "trip_id,stop_sequence")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StopTime {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stop_times_gen")
    @SequenceGenerator(name = "stop_times_gen", sequenceName = "stop_times_seq", allocationSize = 50)
    private Long id;

    @Column(name = "trip_id", nullable = false)
    private String tripId;

    @Column(name = "stop_id", nullable = false)
    private String stopId;

    @Column(name = "arrival_time", nullable = false)
    private LocalTime arrivalTime;

    @Column(name = "departure_time", nullable = false)
    private LocalTime departureTime;

    @Column(name = "stop_sequence", nullable = false)
    private int stopSequence;

    public StopTime(String tripId, String stopId, LocalTime arrivalTime, LocalTime departureTime, int stopSequence) {
        this.tripId = tripId;
        this.stopId = stopId;
        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
        this.stopSequence = stopSequence;
    }
}