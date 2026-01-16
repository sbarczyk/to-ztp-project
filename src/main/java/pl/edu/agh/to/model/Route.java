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
@Table(name = "routes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Route {

    @Id
    @Column(name = "route_id", nullable = false)
    private String routeId;

    @Column(name = "route_short_name", nullable = false)
    private String routeShortName;

    @Column(name = "route_long_name", nullable = false)
    private String routeLongName;

    @Column(name = "route_type", nullable = false)
    private int routeType;
}