package pl.edu.agh.to.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.edu.agh.to.model.CalendarDate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CalendarDateRepository extends JpaRepository<CalendarDate, Long> {

    List<CalendarDate> findByServiceIdAndDate(String serviceId, LocalDate date);
}