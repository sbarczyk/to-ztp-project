package pl.edu.agh.to.model;

import java.time.LocalTime;

public record ConnectionDto(
        String lineNumber,
        String direction,
        String startStop,
        String endStop,
        LocalTime departureTime,
        LocalTime arrivalTime,
        int delayInMinutes,
        LocalTime predictedDeparture
) {}