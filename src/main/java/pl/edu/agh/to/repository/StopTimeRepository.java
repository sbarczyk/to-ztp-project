package pl.edu.agh.to.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pl.edu.agh.to.dbresults.FindNextDeparturesForLineResult;
import pl.edu.agh.to.dbresults.FindNextDeparturesResult;
import pl.edu.agh.to.model.StopTime;

import java.time.LocalTime;
import java.util.List;

@Repository
public interface StopTimeRepository extends JpaRepository<StopTime, Long> {

    @Query("""
        SELECT new pl.edu.agh.to.dbresults.FindNextDeparturesResult(stA, stB, t, r)
        FROM StopTime stA
        JOIN Trip t ON t.tripId = stA.tripId
        JOIN Route r ON r.routeId = t.routeId
        JOIN StopTime stB ON stB.tripId = t.tripId
        WHERE stA.stopId IN :startIds
          AND stB.stopId IN :endIds
          AND stA.stopSequence < stB.stopSequence
          AND stA.departureTime >= :startTime
          AND t.serviceId IN :activeServices
        ORDER BY stA.departureTime ASC
    """)
    List<FindNextDeparturesResult> findNextDepartures(
            List<String> startIds,
            List<String> endIds,
            LocalTime startTime,
            List<String> activeServices,
            Pageable pageable
    );

    @Query("""
        SELECT new pl.edu.agh.to.dbresults.FindNextDeparturesForLineResult(st, t)
        FROM StopTime st
        JOIN Trip t ON t.tripId = st.tripId
        JOIN Route r ON r.routeId = t.routeId
        WHERE st.stopId IN :stopIds
          AND r.routeShortName = :lineNumber
          AND st.departureTime >= :startTime
          AND t.serviceId IN :activeServices
        ORDER BY st.departureTime ASC
    """)
    List<FindNextDeparturesForLineResult> findNextDeparturesForLine(
            List<String> stopIds,
            String lineNumber,
            LocalTime startTime,
            List<String> activeServices,
            Pageable pageable
    );
}