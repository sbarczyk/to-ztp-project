package pl.edu.agh.to.gtfs.statics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.edu.agh.to.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StaticGtfsParserTest {

    private final StaticGtfsParser parser = new StaticGtfsParser();

    @TempDir
    Path tempDir;

    @Test
    void shouldParseStopsCorrectly() throws IOException {
        // Given
        Path file = tempDir.resolve("stops.txt");
        // Format zgodny z Twoim kodem: p[0]=id, p[2]=name, p[4]=lat, p[5]=lon
        String content = """
                stop_id,stop_code,stop_name,stop_desc,stop_lat,stop_lon
                "123",,"Przystanek Testowy",,50.061,19.937
                """;
        Files.writeString(file, content);

        // When
        List<Stop> stops = parser.parseStops(file);

        // Then
        assertThat(stops).hasSize(1);
        Stop stop = stops.get(0);
        assertThat(stop.getId()).isEqualTo("123");
        assertThat(stop.getName()).isEqualTo("Przystanek Testowy");
        assertThat(stop.getLat()).isEqualTo(50.061);
        assertThat(stop.getLon()).isEqualTo(19.937);
    }

    @Test
    void shouldParseTripsCorrectly() throws IOException {
        // Given
        Path file = tempDir.resolve("trips.txt");
        // Twoja logika w kodzie: list.add(new Trip(p[2], p[0], p[1]));
        // Zakładamy konstruktor Trip(tripId, routeId, serviceId)
        // Czyli CSV musi być: 0=route_id, 1=service_id, 2=trip_id (Format Tramwajowy)
        String content = """
                route_id,service_id,trip_id
                route_1,service_A,trip_100
                """;
        Files.writeString(file, content);

        // When
        List<Trip> trips = parser.parseTrips(file);

        // Then
        assertThat(trips).hasSize(1);
        Trip trip = trips.get(0);
        assertThat(trip.getTripId()).isEqualTo("trip_100");   // p[2]
        assertThat(trip.getRouteId()).isEqualTo("route_1");   // p[0]
        assertThat(trip.getServiceId()).isEqualTo("service_A"); // p[1]
    }

    @Test
    void shouldParseStopTimesAndHandleTimeOver24h() throws IOException {
        // Given
        Path file = tempDir.resolve("stop_times.txt");
        // Twoja logika: p[0]=tripId, p[1]=arrival, p[2]=departure, p[3]=stopId, p[4]=seq
        String content = """
                trip_id,arrival_time,departure_time,stop_id,stop_sequence
                trip_100,23:59:00,25:01:00,stop_1,1
                """;
        Files.writeString(file, content);

        // When
        List<StopTime> stopTimes = parser.parseStopTimes(file);

        // Then
        assertThat(stopTimes).hasSize(1);
        StopTime st = stopTimes.get(0);
        assertThat(st.getArrivalTime()).isEqualTo(LocalTime.of(23, 59, 0));
        // Testujemy czy 25:01 zmieniło się na 01:01
        assertThat(st.getDepartureTime()).isEqualTo(LocalTime.of(1, 1, 0));
    }

    @Test
    void shouldParseCalendarCorrectly() throws IOException {
        // Given
        Path file = tempDir.resolve("calendar.txt");
        // Twoja logika: p[0]=id, p[1]..p[7]=dni, p[8]=start, p[9]=end
        String content = """
                service_id,monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date
                service_A,1,0,1,0,1,0,0,20250101,20251231
                """;
        Files.writeString(file, content);

        // When
        List<Calendar> calendars = parser.parseCalendar(file);

        // Then
        assertThat(calendars).hasSize(1);
        Calendar cal = calendars.get(0);
        assertThat(cal.getServiceId()).isEqualTo("service_A");
        assertThat(cal.getOperatingDays())
                .containsExactlyInAnyOrder(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
    }

    @Test
    void shouldParseCalendarDatesCorrectly() throws IOException {
        // Given
        Path file = tempDir.resolve("calendar_dates.txt");
        // Format z Twojego kodu: p[0]=service_id, p[1]=date (yyyyMMdd), p[2]=exception_type
        String content = """
                service_id,date,exception_type
                service_A,20250101,1
                service_B,20251231,2
                """;
        Files.writeString(file, content);

        // When
        List<CalendarDate> calendarDates = parser.parseCalendarDates(file);

        // Then
        assertThat(calendarDates).hasSize(2);

        CalendarDate cd1 = calendarDates.get(0);
        assertThat(cd1.getServiceId()).isEqualTo("service_A");
        assertThat(cd1.getDate()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(cd1.getExceptionType()).isEqualTo(1);

        CalendarDate cd2 = calendarDates.get(1);
        assertThat(cd2.getServiceId()).isEqualTo("service_B");
        assertThat(cd2.getDate()).isEqualTo(LocalDate.of(2025, 12, 31));
        assertThat(cd2.getExceptionType()).isEqualTo(2);
    }

    @Test
    void shouldReturnEmptyList_WhenCalendarDatesFileDoesNotExist() throws IOException {
        // Given
        Path nonExistentFile = tempDir.resolve("non_existent.txt");

        // When
        List<CalendarDate> result = parser.parseCalendarDates(nonExistentFile);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldParseRoutesCorrectly() throws IOException {
        // Given
        Path file = tempDir.resolve("routes.txt");
        // Logika z Twojego screena:
        // new Route(p[0], p[2], p[3], Integer.parseInt(p[4]))
        // p[1] jest pomijane (zazwyczaj agency_id)

        // CSV: route_id, agency_id, route_short_name, route_long_name, route_type
        String content = """
                route_id,agency_id,route_short_name,route_long_name,route_type
                route_1,agency_99,501,Chełmońskiego Pętla,3
                """;
        Files.writeString(file, content);

        // When
        List<Route> routes = parser.parseRoutes(file);

        // Then
        assertThat(routes).hasSize(1);
        Route route = routes.get(0);

        // p[0]
        assertThat(route.getRouteId()).isEqualTo("route_1");
        // p[2]
        assertThat(route.getRouteShortName()).isEqualTo("501");
        // p[3]
        assertThat(route.getRouteLongName()).isEqualTo("Chełmońskiego Pętla");
        // p[4]
        assertThat(route.getRouteType()).isEqualTo(3);
    }
}