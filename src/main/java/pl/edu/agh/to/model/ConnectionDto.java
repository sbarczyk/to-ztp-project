package pl.edu.agh.to.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;

public record ConnectionDto(
        String lineNumber,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
        LocalTime scheduledDeparture,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
        LocalTime predictedDeparture,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
        LocalTime predictedArrival,
        long delaySeconds
) { }