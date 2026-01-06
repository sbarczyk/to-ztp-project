package pl.edu.agh.to.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "calendar_dates", indexes = {
        @Index(name = "idx_cal_date_service", columnList = "serviceId")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class CalendarDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Sztuczne ID dla bazy danych

    private String serviceId;
    private LocalDate date;
    private int exceptionType; // 1 = dodany, 2 = usunięty

    // Konstruktor pomocniczy, żeby parser działał bez zmian
    public CalendarDate(String serviceId, LocalDate date, int exceptionType) {
        this.serviceId = serviceId;
        this.date = date;
        this.exceptionType = exceptionType;
    }
}