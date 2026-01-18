package pl.edu.agh.to.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;

public record NextDepartureDto(
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
        LocalTime scheduledDeparture,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
        LocalTime predictedDeparture,
        long delaySeconds
) { }