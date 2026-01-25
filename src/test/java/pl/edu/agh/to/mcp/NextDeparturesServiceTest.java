package pl.edu.agh.to.mcp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import pl.edu.agh.to.dbresults.FindNextDeparturesForLineResult;
import pl.edu.agh.to.dto.DeparturesResponseDto;
import pl.edu.agh.to.dto.NextDepartureDto;
import pl.edu.agh.to.gtfs.realtime.GtfsDelayService;
import pl.edu.agh.to.model.Stop;
import pl.edu.agh.to.model.StopTime;
import pl.edu.agh.to.model.Trip;
import pl.edu.agh.to.repository.StopRepository;
import pl.edu.agh.to.repository.StopTimeRepository;
import pl.edu.agh.to.service.ActiveServiceResolver;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NextDeparturesServiceTest {

    @Mock
    private StopTimeRepository stopTimeRepository;
    @Mock
    private StopRepository stopRepository;
    @Mock
    private ActiveServiceResolver activeServiceResolver;
    @Mock
    private GtfsDelayService delayService;

    @InjectMocks
    private NextDeparturesService service;

    @Test
    void shouldReturnNextDeparturesWithDelays() {
        // given
        String stopName = "Teatr Bagatela";
        String line = "4";
        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.of(10, 0);

        when(stopRepository.findByName(stopName)).thenReturn(List.of(new Stop("s1", stopName, 0, 0)));

        when(activeServiceResolver.getActiveServiceIds(date)).thenReturn(List.of("service1"));

        when(delayService.getCurrentDelays()).thenReturn(Map.of("T1", 120L));

        FindNextDeparturesForLineResult row = createDbRow("T1", LocalTime.of(10, 5));

        when(stopTimeRepository.findNextDeparturesForLine(
                eq(List.of("s1")),
                eq("4"),
                eq(time),
                anyList(),
                any(Pageable.class)
        )).thenReturn(Collections.singletonList(row));

        // when
        DeparturesResponseDto result = service.getNext5Departures(stopName, line, date, time);

        // then
        assertThat(result.departures()).hasSize(1);
        NextDepartureDto dep = result.departures().get(0);

        assertThat(dep.scheduledDeparture()).isEqualTo(LocalTime.of(10, 5));
        assertThat(dep.predictedDeparture()).isEqualTo(LocalTime.of(10, 7)); // 10:05 + 2min

        assertThat(dep.delaySeconds()).isEqualTo(120L);
    }

    @Test
    void shouldReturnEmptyWhenStopNotFound() {
        // given
        String stopName = "Unknown";
        when(stopRepository.findByName(stopName)).thenReturn(Collections.emptyList());

        // when
        DeparturesResponseDto result = service.getNext5Departures(stopName, "4", null, null);

        // then
        assertThat(result.departures()).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenInputsAreNull() {
        // when
        DeparturesResponseDto result = service.getNext5Departures(null, null, null, null);

        // then
        assertThat(result.departures()).isEmpty();
    }

    private FindNextDeparturesForLineResult createDbRow(String tripId, LocalTime departure) {
        Trip trip = new Trip();
        trip.setTripId(tripId);

        StopTime stopTime = new StopTime();
        stopTime.setDepartureTime(departure);

        return new FindNextDeparturesForLineResult(stopTime, trip);
    }
}