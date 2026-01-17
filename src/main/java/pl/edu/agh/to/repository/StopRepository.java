package pl.edu.agh.to.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pl.edu.agh.to.model.Stop;

import java.util.List;

@Repository
public interface StopRepository extends JpaRepository<Stop, String> {

    List<Stop> findByName(String name);

    @Query("""
        SELECT DISTINCT s.name
        FROM Stop s
        WHERE (:lastStopName IS NULL OR s.name > :lastStopName)
        ORDER BY s.name
    """)
    Slice<String> findDistinctNamesAfter(String lastStopName, Pageable pageable);

    @Query("""
        SELECT DISTINCT s.name
        FROM Stop s
        WHERE lower(s.name) LIKE lower(concat(:prefix, '%'))
          AND (:lastStopName IS NULL OR s.name > :lastStopName)
        ORDER BY s.name
    """)
    Slice<String> findDistinctNamesByPrefixAfter(String prefix, String lastStopName, Pageable pageable);
}