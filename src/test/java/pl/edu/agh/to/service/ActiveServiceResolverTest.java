package pl.edu.agh.to.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.edu.agh.to.model.Calendar;
import pl.edu.agh.to.model.CalendarDate;
import pl.edu.agh.to.repository.CalendarDateRepository;
import pl.edu.agh.to.repository.CalendarRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActiveServiceResolverTest {

    @Mock
    private CalendarRepository calendarRepository;

    @Mock
    private CalendarDateRepository calendarDateRepository;

    @InjectMocks
    private ActiveServiceResolver resolver;

    @Test
    void shouldReturnActiveServiceForStandardSchedule() {
        LocalDate date = LocalDate.of(2024, 6, 3); // Monday
        Calendar calendar = new Calendar("S1", Set.of(DayOfWeek.MONDAY), date.minusDays(1), date.plusDays(1));

        when(calendarRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(date, date))
                .thenReturn(List.of(calendar));
        when(calendarDateRepository.findByDate(date)).thenReturn(Collections.emptyList());

        List<String> result = resolver.getActiveServiceIds(date);

        assertThat(result).containsExactly("S1");
    }

    @Test
    void shouldNotReturnServiceForWrongDayOfWeek() {
        LocalDate date = LocalDate.of(2024, 6, 3); // Monday
        Calendar calendar = new Calendar("S1", Set.of(DayOfWeek.TUESDAY), date.minusDays(1), date.plusDays(1));

        when(calendarRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(date, date))
                .thenReturn(List.of(calendar));
        when(calendarDateRepository.findByDate(date)).thenReturn(Collections.emptyList());

        List<String> result = resolver.getActiveServiceIds(date);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldAddServiceFromExceptionType1() {
        LocalDate date = LocalDate.of(2024, 6, 3);
        CalendarDate exception = new CalendarDate("S_EXTRA", date, 1);

        when(calendarRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(date, date))
                .thenReturn(Collections.emptyList());
        when(calendarDateRepository.findByDate(date)).thenReturn(List.of(exception));

        List<String> result = resolver.getActiveServiceIds(date);

        assertThat(result).containsExactly("S_EXTRA");
    }

    @Test
    void shouldRemoveServiceFromExceptionType2() {
        LocalDate date = LocalDate.of(2024, 6, 3); // Monday
        Calendar calendar = new Calendar("S1", Set.of(DayOfWeek.MONDAY), date.minusDays(1), date.plusDays(1));
        CalendarDate exception = new CalendarDate("S1", date, 2);

        when(calendarRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(date, date))
                .thenReturn(List.of(calendar));
        when(calendarDateRepository.findByDate(date)).thenReturn(List.of(exception));

        List<String> result = resolver.getActiveServiceIds(date);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldHandleMixedAdditionsAndRemovals() {
        LocalDate date = LocalDate.of(2024, 6, 3); // Monday

        Calendar cal1 = new Calendar("S1", Set.of(DayOfWeek.MONDAY), date, date);
        Calendar cal2 = new Calendar("S2", Set.of(DayOfWeek.MONDAY), date, date);

        CalendarDate exRemoveS2 = new CalendarDate("S2", date, 2);
        CalendarDate exAddS3 = new CalendarDate("S3", date, 1);

        when(calendarRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(date, date))
                .thenReturn(List.of(cal1, cal2));
        when(calendarDateRepository.findByDate(date)).thenReturn(List.of(exRemoveS2, exAddS3));

        List<String> result = resolver.getActiveServiceIds(date);

        assertThat(result).containsExactlyInAnyOrder("S1", "S3");
    }
}
