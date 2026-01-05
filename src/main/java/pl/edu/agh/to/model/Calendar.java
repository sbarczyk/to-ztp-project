package pl.edu.agh.to.model;

import lombok.Value;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

@Value
public class Calendar {
    String serviceId;
    Set<DayOfWeek> operatingDays;
    LocalDate startDate;
    LocalDate endDate;
}