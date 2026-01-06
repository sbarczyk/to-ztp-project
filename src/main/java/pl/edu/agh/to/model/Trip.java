package pl.edu.agh.to.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "trips")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Trip {
    @Id
    private String tripId; // trip_id

    private String routeId;
    private String serviceId;
}