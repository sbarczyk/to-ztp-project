package pl.edu.agh.to.model;

import lombok.Value;
import java.time.LocalDate;

@Value
public class CalendarDate {
    String serviceId;
    LocalDate date;
    int exceptionType;
}