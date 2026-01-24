package pl.edu.agh.to.mcp;

import pl.edu.agh.to.exceptions.BadRequestException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

class DateTimeParamParser {

    private DateTimeParamParser() {}  // utility class

    public static LocalDate parseDateOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new BadRequestException(
                    "Invalid date format. Expected yyyy-MM-dd, got: " + value
            );
        }
    }

    public static LocalTime parseTimeOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new BadRequestException(
                    "Invalid time format. Expected HH:mm or HH:mm:ss, got: " + value
            );
        }
    }
}