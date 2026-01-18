package pl.edu.agh.to.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

@Entity
@Table(name = "calendar")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Calendar {

    @Id
    @Column(name = "service_id", nullable = false)
    private String serviceId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "calendar_operating_days", joinColumns = @JoinColumn(name = "service_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "day", nullable = false)
    private Set<DayOfWeek> operatingDays;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;


    public boolean operatesOn(DayOfWeek day) {
        return operatingDays != null && operatingDays.contains(day);
    }
}