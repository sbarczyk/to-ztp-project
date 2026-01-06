package pl.edu.agh.to.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

@Entity
@Table(name = "calendar")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Calendar {

    @Id
    private String serviceId; // W pliku calendar.txt service_id jest unikalne

    @ElementCollection(fetch = FetchType.EAGER) // Pobieraj dni od razu
    @CollectionTable(name = "calendar_operating_days", joinColumns = @JoinColumn(name = "service_id"))
    @Enumerated(EnumType.STRING) // Zapisuj w bazie jako napisy "MONDAY", "TUESDAY"
    private Set<DayOfWeek> operatingDays;

    private LocalDate startDate;
    private LocalDate endDate;
}