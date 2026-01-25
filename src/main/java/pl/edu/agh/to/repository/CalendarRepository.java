package pl.edu.agh.to.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.edu.agh.to.model.Calendar;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CalendarRepository extends JpaRepository<Calendar, String> {

    List<Calendar> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate dateFrom, LocalDate dateTo);
}