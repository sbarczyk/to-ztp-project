package pl.edu.agh.to.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.edu.agh.to.exceptions.BadRequestException;
import pl.edu.agh.to.exceptions.NotFoundException;
import pl.edu.agh.to.gtfs.realtime.GtfsDelayService;
import pl.edu.agh.to.model.*;
import pl.edu.agh.to.model.Calendar;
import pl.edu.agh.to.dto.RouteSearchResultDto;
import pl.edu.agh.to.repository.CalendarDateRepository;
import pl.edu.agh.to.repository.CalendarRepository;
import pl.edu.agh.to.repository.StopRepository;
import pl.edu.agh.to.repository.TripRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteServiceTest {

    @Mock
    private StopRepository stopRepository;
    @Mock
    private TripRepository tripRepository;
    @Mock
    private CalendarRepository calendarRepository;
    @Mock
    private CalendarDateRepository calendarDateRepository;
    @Mock
    private GtfsDelayService delayService;

    @InjectMocks
    private RouteService routeService;

    private final LocalDate testDate = LocalDate.of(2026, 1, 8);
    private final LocalTime testNow = LocalTime.of(10, 0);

    @Test
    void shouldThrowBadRequestExceptionWhenStartNameIsNull() {
        // Given
        String start = null;

        // When
        Executable action = () -> routeService.findFastestConnection(start, "Kawiory");

        // Then
        assertThrows(BadRequestException.class, action);
    }

    @Test
    void shouldThrowBadRequestExceptionWhenStartNameIsBlank() {
        // Given
        String start = "   ";

        // When
        Executable action = () -> routeService.findFastestConnection(start, "Kawiory");

        // Then
        assertThrows(BadRequestException.class, action);
    }

    @Test
    void shouldThrowBadRequestExceptionWhenStopNotFound() {
        // Given
        when(stopRepository.findByName(anyString())).thenReturn(Collections.emptyList());

        // When
        Executable action = () -> routeService.findFastestConnection("Invalid", "Kawiory");

        // Then
        assertThrows(BadRequestException.class, action);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenNoDirectConnectionsInDb() {
        // Given
        setupStopsMocks("A", "B");
        when(tripRepository.findDirectConnectionsWithDetails(anyList(), anyList())).thenReturn(Collections.emptyList());
        when(delayService.getCurrentDelays()).thenReturn(Collections.emptyMap());

        // When
        Executable action = () -> routeService.findFastestConnection("A", "B");

        // Then
        assertThrows(NotFoundException.class, action);
    }

    @Test
    void shouldReturnFastestConnectionWhenValidTripsExist() {
        // Given
        setupStopsMocks("Start", "End");

        Trip trip = new Trip("T1", "R1", "S1");
        LocalTime dep = LocalTime.of(10, 30);
        LocalTime arr = LocalTime.of(11, 0);
        StopTime stS = new StopTime("T1", "S1", dep, dep, 1);
        StopTime stE = new StopTime("T1", "S2", arr, arr, 2);
        Route route = new Route("R1", "159", "L1", 3);

        Object[] row = new Object[]{trip, stS, stE, route};
        when(tripRepository.findDirectConnectionsWithDetails(anyList(), anyList()))
                .thenReturn(Collections.singletonList(row));

        when(delayService.getCurrentDelays()).thenReturn(Collections.emptyMap());
        when(calendarDateRepository.findByServiceIdAndDate(anyString(), any())).thenReturn(Collections.emptyList());
        when(calendarRepository.findById("S1")).thenReturn(Optional.of(
                new Calendar("S1", Set.of(DayOfWeek.THURSDAY), testDate.minusDays(1), testDate.plusDays(1))
        ));

        // When
        RouteSearchResultDto result;
        try (MockedStatic<LocalDate> ld = mockStatic(LocalDate.class, Answers.CALLS_REAL_METHODS);
             MockedStatic<LocalTime> lt = mockStatic(LocalTime.class, Answers.CALLS_REAL_METHODS)) {
            ld.when(LocalDate::now).thenReturn(testDate);
            lt.when(LocalTime::now).thenReturn(testNow);
            result = routeService.findFastestConnection("Start", "End");
        }

        // Then
        assertNotNull(result);
        assertEquals("T1", result.getTripId());
        assertEquals(arr, result.getArrivalTime());
    }

    @Test
    void shouldApplyDelayAndFindEarlierArrival() {
        // Given
        setupStopsMocks("Start", "End");

        Trip t1 = new Trip("T1", "R1", "S1");
        LocalTime t1S = LocalTime.of(10, 10);
        LocalTime t1E = LocalTime.of(10, 20);
        StopTime st1S = new StopTime("T1", "S1", t1S, t1S, 1);
        StopTime st1E = new StopTime("T1", "S2", t1E, t1E, 2);
        Route r1 = new Route("R1", "1", "L1", 3);

        Trip t2 = new Trip("T2", "R2", "S1");
        LocalTime t2S = LocalTime.of(10, 05);
        LocalTime t2E = LocalTime.of(10, 15);
        StopTime st2S = new StopTime("T2", "S1", t2S, t2S, 1);
        StopTime st2E = new StopTime("T2", "S2", t2E, t2E, 2);
        Route r2 = new Route("R2", "2", "L2", 3);

        when(tripRepository.findDirectConnectionsWithDetails(anyList(), anyList()))
                .thenReturn(List.of(new Object[]{t1, st1S, st1E, r1}, new Object[]{t2, st2S, st2E, r2}));

        when(delayService.getCurrentDelays()).thenReturn(Map.of("T2", 600L));
        when(calendarDateRepository.findByServiceIdAndDate(anyString(), any())).thenReturn(Collections.emptyList());
        when(calendarRepository.findById("S1")).thenReturn(Optional.of(
                new Calendar("S1", Set.of(DayOfWeek.THURSDAY), testDate.minusDays(1), testDate.plusDays(1))
        ));

        // When
        RouteSearchResultDto result;
        try (MockedStatic<LocalDate> ld = mockStatic(LocalDate.class, Answers.CALLS_REAL_METHODS);
             MockedStatic<LocalTime> lt = mockStatic(LocalTime.class, Answers.CALLS_REAL_METHODS)) {
            ld.when(LocalDate::now).thenReturn(testDate);
            lt.when(LocalTime::now).thenReturn(testNow);
            result = routeService.findFastestConnection("Start", "End");
        }

        // Then
        assertEquals("T1", result.getTripId());
    }

    @Test
    void shouldReturnTrueWhenTripAddedByExceptionType1() {
        // Given
        setupStopsMocks("Start", "End");
        Trip t1 = new Trip("T1", "R1", "S1");
        LocalTime dep = LocalTime.of(11, 0);
        StopTime stS = new StopTime("T1", "S1", dep, dep, 1);
        StopTime stE = new StopTime("T1", "S2", dep.plusMinutes(10), dep.plusMinutes(10), 2);
        Route r = new Route("R1", "1", "L1", 3);

        Object[] row = new Object[]{t1, stS, stE, r};
        when(tripRepository.findDirectConnectionsWithDetails(anyList(), anyList()))
                .thenReturn(Collections.singletonList(row));

        when(calendarDateRepository.findByServiceIdAndDate(eq("S1"), any(LocalDate.class)))
                .thenReturn(List.of(new CalendarDate("S1", testDate, 1)));

        when(delayService.getCurrentDelays()).thenReturn(Collections.emptyMap());

        // When
        RouteSearchResultDto result;
        try (MockedStatic<LocalDate> ld = mockStatic(LocalDate.class, Answers.CALLS_REAL_METHODS);
             MockedStatic<LocalTime> lt = mockStatic(LocalTime.class, Answers.CALLS_REAL_METHODS)) {
            ld.when(LocalDate::now).thenReturn(testDate);
            lt.when(LocalTime::now).thenReturn(testNow);
            result = routeService.findFastestConnection("Start", "End");
        }

        // Then
        assertNotNull(result);
        assertEquals("T1", result.getTripId());
    }

    @Test
    void shouldThrowNotFoundExceptionWhenTripRemovedByExceptionType2() {
        // Given
        setupStopsMocks("Start", "End");
        Trip t1 = new Trip("T1", "R1", "S1");
        LocalTime dep = LocalTime.of(11, 0);
        StopTime stS = new StopTime("T1", "S1", dep, dep, 1);
        StopTime stE = new StopTime("T1", "S2", dep.plusMinutes(10), dep.plusMinutes(10), 2);
        Route r = new Route("R1", "1", "L1", 3);

        Object[] row = new Object[]{t1, stS, stE, r};
        when(tripRepository.findDirectConnectionsWithDetails(anyList(), anyList()))
                .thenReturn(Collections.singletonList(row));

        when(calendarDateRepository.findByServiceIdAndDate(eq("S1"), any(LocalDate.class)))
                .thenReturn(List.of(new CalendarDate("S1", testDate, 2)));
        when(delayService.getCurrentDelays()).thenReturn(Collections.emptyMap());

        // When
        Executable action;
        try (MockedStatic<LocalDate> ld = mockStatic(LocalDate.class, Answers.CALLS_REAL_METHODS);
             MockedStatic<LocalTime> lt = mockStatic(LocalTime.class, Answers.CALLS_REAL_METHODS)) {
            ld.when(LocalDate::now).thenReturn(testDate);
            lt.when(LocalTime::now).thenReturn(testNow);
            action = () -> routeService.findFastestConnection("Start", "End");
        }

        // Then
        assertThrows(NotFoundException.class, action);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenTripDepartedInThePast() {
        // Given
        setupStopsMocks("Start", "End");
        LocalTime past = testNow.minusMinutes(5);
        Trip t1 = new Trip("T1", "R1", "S1");
        StopTime stS = new StopTime("T1", "S1", past, past, 1);
        StopTime stE = new StopTime("T1", "S2", testNow, testNow, 2);
        Route r = new Route("R1", "1", "L1", 3);

        Object[] row = new Object[]{t1, stS, stE, r};
        when(tripRepository.findDirectConnectionsWithDetails(anyList(), anyList()))
                .thenReturn(Collections.singletonList(row));

        when(delayService.getCurrentDelays()).thenReturn(Collections.emptyMap());
        when(calendarDateRepository.findByServiceIdAndDate(anyString(), any())).thenReturn(Collections.emptyList());
        when(calendarRepository.findById("S1")).thenReturn(Optional.of(
                new Calendar("S1", Set.of(DayOfWeek.THURSDAY), testDate.minusDays(1), testDate.plusDays(1))
        ));

        // When
        Executable action;
        try (MockedStatic<LocalDate> ld = mockStatic(LocalDate.class, Answers.CALLS_REAL_METHODS);
             MockedStatic<LocalTime> lt = mockStatic(LocalTime.class, Answers.CALLS_REAL_METHODS)) {
            ld.when(LocalDate::now).thenReturn(testDate);
            lt.when(LocalTime::now).thenReturn(testNow);
            action = () -> routeService.findFastestConnection("Start", "End");
        }

        // Then
        assertThrows(NotFoundException.class, action);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenDayOfWeekNotInCalendar() {
        // Given
        setupStopsMocks("Start", "End");


        LocalTime departureTime = LocalTime.of(10, 30);
        LocalTime arrivalTime = LocalTime.of(11, 0);

        Trip t1 = new Trip("T1", "R1", "S1");
        StopTime stS = new StopTime("T1", "S1", departureTime, departureTime, 1);
        StopTime stE = new StopTime("T1", "S2", arrivalTime, arrivalTime, 2);
        Route r = new Route("R1", "1", "L1", 3);

        Object[] row = new Object[]{t1, stS, stE, r};
        when(tripRepository.findDirectConnectionsWithDetails(anyList(), anyList()))
                .thenReturn(Collections.singletonList(row));

        when(calendarDateRepository.findByServiceIdAndDate(anyString(), any())).thenReturn(Collections.emptyList());


        when(calendarRepository.findById("S1")).thenReturn(Optional.of(
                new Calendar("S1", Set.of(DayOfWeek.MONDAY), testDate.minusDays(10), testDate.plusDays(10))
        ));

        // When
        Executable action;
        try (MockedStatic<LocalDate> ld = mockStatic(LocalDate.class, Answers.CALLS_REAL_METHODS);
             MockedStatic<LocalTime> lt = mockStatic(LocalTime.class, Answers.CALLS_REAL_METHODS)) {
            ld.when(LocalDate::now).thenReturn(testDate);
            lt.when(LocalTime::now).thenReturn(testNow);

            action = () -> routeService.findFastestConnection("Start", "End");
        }

        // Then
        assertThrows(NotFoundException.class, action);
    }

    private void setupStopsMocks(String start, String end) {
        when(stopRepository.findByName(start)).thenReturn(List.of(new Stop("S1", start, 0, 0)));
        when(stopRepository.findByName(end)).thenReturn(List.of(new Stop("S2", end, 0, 0)));
    }
}