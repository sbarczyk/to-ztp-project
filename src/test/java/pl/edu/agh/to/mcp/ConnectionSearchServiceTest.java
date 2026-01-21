package pl.edu.agh.to.mcp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import pl.edu.agh.to.dbresults.FindNextDeparturesResult;
import pl.edu.agh.to.dto.ConnectionDto;
import pl.edu.agh.to.dto.ConnectionsResponseDto;
import pl.edu.agh.to.gtfs.realtime.GtfsDelayService;
import pl.edu.agh.to.model.Route;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConnectionSearchServiceTest {

    @Mock
    private StopTimeRepository stopTimeRepository;

    @Mock
    private StopRepository stopRepository;

    @Mock
    private ActiveServiceResolver activeServiceResolver;

    @Mock
    private GtfsDelayService delayService;

    @InjectMocks
    private ConnectionSearchService service;

    @Test
    void shouldReturnTop3ConnectionsWithDelays() {
        // given
        String fromStop = "Teatr Bagatela";
        String toStop = "Bronowice Małe";
        LocalDate date = LocalDate.of(2024, 6, 1);
        LocalTime time = LocalTime.of(12, 0);

        when(stopRepository.findByName(fromStop)).thenReturn(List.of(new Stop("id_start", "Teatr Bagatela", 50.0, 19.0)));
        when(stopRepository.findByName(toStop)).thenReturn(List.of(new Stop("id_end", "Bronowice Małe", 50.1, 19.1)));

        List<String> activeServices = List.of("service_1");
        when(activeServiceResolver.getActiveServiceIds(date)).thenReturn(activeServices);

        when(delayService.getCurrentDelays()).thenReturn(Map.of("T1", 60L));

        FindNextDeparturesResult row1 = createDatabaseRow("T1", "4", LocalTime.of(12, 10), LocalTime.of(12, 30));

        when(stopTimeRepository.findNextDepartures(
                eq(List.of("id_start")),
                eq(List.of("id_end")),
                eq(time),
                eq(activeServices),
                any(Pageable.class)
        )).thenReturn(Collections.singletonList(row1));

        // when
        ConnectionsResponseDto response = service.findTop3Connections(fromStop, toStop, date, time);

        // then
        assertThat(response.connections()).hasSize(1);
        ConnectionDto conn = response.connections().getFirst();

        assertThat(conn.lineNumber()).isEqualTo("4");
        assertThat(conn.scheduledDeparture()).isEqualTo(LocalTime.of(12, 10));

        assertThat(conn.predictedDeparture()).isEqualTo(LocalTime.of(12, 11));

        assertThat(conn.predictedArrival()).isEqualTo(LocalTime.of(12, 31));
        assertThat(conn.delaySeconds()).isEqualTo(60L);
    }

    @Test
    void shouldHandleNullInputsGracefully() {
        // when
        ConnectionsResponseDto response = service.findTop3Connections(null, null, null, null);

        // then
        assertThat(response.connections()).isEmpty();
        assertThat(response.date()).isNotNull();
        assertThat(response.time()).isNotNull();

        verifyNoInteractions(stopRepository, stopTimeRepository);
    }

    @Test
    void shouldReturnEmptyListWhenStopNotFound() {
        // given
        String fromStop = "Nieznany Przystanek";
        String toStop = "Bronowice";

        when(stopRepository.findByName(fromStop)).thenReturn(Collections.emptyList());

        // when
        ConnectionsResponseDto response = service.findTop3Connections(fromStop, toStop, LocalDate.now(), LocalTime.now());

        // then
        assertThat(response.connections()).isEmpty();
        verify(stopTimeRepository, never()).findNextDepartures(any(), any(), any(), any(), any());
    }

    @Test
    void shouldReturnEmptyListWhenNoConnectionsFound() {
        // given
        String fromStop = "A";
        String toStop = "B";
        LocalDate date = LocalDate.now();

        when(stopRepository.findByName(fromStop)).thenReturn(List.of(new Stop("1", "A", 0, 0)));
        when(stopRepository.findByName(toStop)).thenReturn(List.of(new Stop("2", "B", 0, 0)));
        when(activeServiceResolver.getActiveServiceIds(date)).thenReturn(List.of("S1"));
        when(delayService.getCurrentDelays()).thenReturn(Map.of());

        when(stopTimeRepository.findNextDepartures(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // when
        ConnectionsResponseDto response = service.findTop3Connections(fromStop, toStop, date, LocalTime.now());

        // then
        assertThat(response.connections()).isEmpty();
    }

    @Test
    void shouldUseCurrentDateAndTimeWhenInputsAreNull() {
        // given
        String from = "A";
        String to = "B";

        when(stopRepository.findByName(any())).thenReturn(List.of(new Stop("id", "name", 0, 0)));
        when(activeServiceResolver.getActiveServiceIds(any(LocalDate.class))).thenReturn(List.of("S1"));

        // when
        service.findTop3Connections(from, to, null, null);

        // then
        verify(stopTimeRepository).findNextDepartures(
                anyList(),
                anyList(),
                any(LocalTime.class),
                anyList(),
                eq(PageRequest.of(0, 3))
        );
        verify(activeServiceResolver).getActiveServiceIds(any(LocalDate.class));
    }

    private FindNextDeparturesResult createDatabaseRow(String tripId, String routeName, LocalTime departure, LocalTime arrival) {
        Trip trip = new Trip();
        trip.setTripId(tripId);

        Route route = new Route();
        route.setRouteShortName(routeName);

        StopTime startSt = new StopTime();
        startSt.setDepartureTime(departure);

        StopTime endSt = new StopTime();
        endSt.setArrivalTime(arrival);

        return new FindNextDeparturesResult(startSt, endSt, trip, route);
    }
}