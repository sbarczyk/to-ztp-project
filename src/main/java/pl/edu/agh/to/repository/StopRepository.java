package pl.edu.agh.to.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.edu.agh.to.model.Stop;
import java.util.List;

@Repository
public interface StopRepository extends JpaRepository<Stop, String> {
    List<Stop> findByName(String name);
}
