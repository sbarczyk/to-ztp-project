package pl.edu.agh.to.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.edu.agh.to.model.StopTime;
import java.util.List;

@Repository
public interface StopTimeRepository extends JpaRepository<StopTime, Long> {
    // Szybkie wyciąganie czasów dla jednego tripa (posortowane!)
    List<StopTime> findByTripIdOrderByStopSequence(String tripId);
}