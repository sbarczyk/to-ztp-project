package pl.edu.agh.to.mcp;

import org.junit.jupiter.api.Test;
import pl.edu.agh.to.exceptions.BadRequestException;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateTimeParamParserTest {

    @Test
    void shouldParseValidDate() {
        // given
        String input = "2024-05-20";

        // when
        LocalDate result = DateTimeParamParser.parseDateOrNull(input);

        // then
        assertThat(result).isEqualTo(LocalDate.of(2024, 5, 20));
    }

    @Test
    void shouldReturnNullForNullDateInput() {
        // when
        LocalDate result = DateTimeParamParser.parseDateOrNull(null);

        // then
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullForBlankDateInput() {
        // when
        LocalDate result = DateTimeParamParser.parseDateOrNull("   ");

        // then
        assertThat(result).isNull();
    }

    @Test
    void shouldThrowExceptionForInvalidDateFormat() {
        // given
        String input = "20-05-2024";

        // when & then
        assertThatThrownBy(() -> DateTimeParamParser.parseDateOrNull(input))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid date format");
    }


    @Test
    void shouldParseValidTime() {
        // given
        String input = "14:30";

        // when
        LocalTime result = DateTimeParamParser.parseTimeOrNull(input);

        // then
        assertThat(result).isEqualTo(LocalTime.of(14, 30));
    }

    @Test
    void shouldParseValidTimeWithSeconds() {
        // given
        String input = "14:30:15";

        // when
        LocalTime result = DateTimeParamParser.parseTimeOrNull(input);

        // then
        assertThat(result).isEqualTo(LocalTime.of(14, 30, 15));
    }

    @Test
    void shouldReturnNullForNullTimeInput() {
        // when
        LocalTime result = DateTimeParamParser.parseTimeOrNull(null);

        // then
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullForBlankTimeInput() {
        // when
        LocalTime result = DateTimeParamParser.parseTimeOrNull("   ");

        // then
        assertThat(result).isNull();
    }

    @Test
    void shouldThrowExceptionForInvalidTimeFormat() {
        // given
        String input = "25:00";

        // when & then
        assertThatThrownBy(() -> DateTimeParamParser.parseTimeOrNull(input))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid time format");
    }
}