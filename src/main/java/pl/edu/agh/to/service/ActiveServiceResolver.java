package pl.edu.agh.to.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.to.model.Calendar;
import pl.edu.agh.to.model.CalendarDate;
import pl.edu.agh.to.repository.CalendarDateRepository;
import pl.edu.agh.to.repository.CalendarRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ActiveServiceResolver {

    private final CalendarRepository calendarRepository;
    private final CalendarDateRepository calendarDateRepository;

    @Transactional(readOnly = true)
    public List<String> getActiveServiceIds(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();

        // Load only calendars valid for the given date range, then filter by weekday in-memory
        List<Calendar> candidates = calendarRepository
                .findByStartDateLessThanEqualAndEndDateGreaterThanEqual(date, date);

        Set<String> active = new HashSet<>();
        for (Calendar c : candidates) {
            if (operatesOn(c, day)) {
                active.add(c.getServiceId());
            }
        }

        // Apply exceptions only for the requested date (no full table scan)
        List<CalendarDate> exceptions = calendarDateRepository.findByDate(date);
        for (CalendarDate ex : exceptions) {
            if (ex.getExceptionType() == 1) {
                active.add(ex.getServiceId());
            } else if (ex.getExceptionType() == 2) {
                active.remove(ex.getServiceId());
            }
        }

        return active.stream().toList();
    }

    private boolean operatesOn(Calendar c, DayOfWeek day) {
        return c.getOperatingDays() != null && c.getOperatingDays().contains(day);
    }
}