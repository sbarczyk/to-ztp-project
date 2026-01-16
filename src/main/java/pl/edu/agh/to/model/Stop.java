package pl.edu.agh.to.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stops")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Stop {

    @Id
    @Column(name = "stop_id", nullable = false)
    private String id;

    @Column(name = "stop_name", nullable = false)
    private String name;

    @Column(name = "stop_lat", nullable = false)
    private double lat;

    @Column(name = "stop_lon", nullable = false)
    private double lon;
}