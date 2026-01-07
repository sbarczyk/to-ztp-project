package pl.edu.agh.to.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pl.edu.agh.to.model.Trip;

import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, String> {

    /**
     * Finds direct connections between stops and fetches associated Route information.
     * This join prevents the N+1 problem by retrieving all necessary data in one query.
     */
    @Query("""
        SELECT t, stStart, stEnd, r 
        FROM Trip t
        JOIN StopTime stStart ON t.tripId = stStart.tripId
        JOIN StopTime stEnd ON t.tripId = stEnd.tripId
        LEFT JOIN Route r ON t.routeId = r.routeId
        WHERE stStart.stopId IN :startStopIds
          AND stEnd.stopId IN :endStopIds
          AND stStart.stopSequence < stEnd.stopSequence
    """)
    List<Object[]> findDirectConnectionsWithDetails(List<String> startStopIds, List<String> endStopIds);
}