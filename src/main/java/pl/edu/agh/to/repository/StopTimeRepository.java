package pl.edu.agh.to.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pl.edu.agh.to.model.StopTime;

import java.time.LocalTime;
import java.util.List;

@Repository
public interface StopTimeRepository extends JpaRepository<StopTime, Long> {

    // Zapytanie szukające par: Przystanek Startowy -> Przystanek Końcowy w ramach jednego Tripa
    // Zwraca: [0]StopTimeStart, [1]StopTimeEnd, [2]Trip, [3]Route
    @Query("""
        SELECT stA, stB, t, r
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
    List<Object[]> findNextDepartures(
            List<String> startIds,      // <-- ZMIANA: Lista ID zamiast nazwy
            List<String> endIds,        // <-- ZMIANA: Lista ID zamiast nazwy
            LocalTime startTime,
            List<String> activeServices,
            Pageable pageable
    );
}