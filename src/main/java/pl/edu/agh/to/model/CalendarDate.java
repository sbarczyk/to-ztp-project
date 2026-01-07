package pl.edu.agh.to.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(
        name = "calendar_dates",
        indexes = @Index(name = "idx_cal_date_service_date", columnList = "service_id,date")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CalendarDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_id", nullable = false)
    private String serviceId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "exception_type", nullable = false)
    private int exceptionType;

    public CalendarDate(String serviceId, LocalDate date, int exceptionType) {
        this.serviceId = serviceId;
        this.date = date;
        this.exceptionType = exceptionType;
    }
}