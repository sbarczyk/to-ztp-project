package pl.edu.agh.to.model;

import java.time.LocalTime;

public record NextDepartureDto(
        String lineNumber,       // np. "4"
        String direction,        // np. "Bronowice Małe"
        LocalTime scheduledTime, // Rozkładowo: 14:15
        LocalTime predictedTime, // Rzeczywiście: 14:17
        int delayMinutes         // Opóźnienie: 2 min
) {}