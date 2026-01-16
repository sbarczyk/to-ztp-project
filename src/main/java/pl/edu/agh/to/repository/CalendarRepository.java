package pl.edu.agh.to.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.edu.agh.to.model.Calendar;

@Repository
public interface CalendarRepository extends JpaRepository<Calendar, String> {

}