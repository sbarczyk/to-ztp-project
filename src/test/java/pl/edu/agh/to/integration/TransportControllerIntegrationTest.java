package pl.edu.agh.to.integration;

import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.edu.agh.to.controller.TransportController;
import pl.edu.agh.to.exceptions.ExternalServiceException;
import pl.edu.agh.to.exceptions.NotFoundException;
import pl.edu.agh.to.dto.RandomDepartureDto;
import pl.edu.agh.to.dto.RouteSearchResultDto;
import pl.edu.agh.to.mcp.McpToolsController;
import pl.edu.agh.to.service.RandomDepartureService;
import pl.edu.agh.to.service.RouteService;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;


@WebMvcTest(TransportController.class)
class TransportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private RandomDepartureService randomDepartureService;
    @MockitoBean
    private RouteService routeService;
    @MockitoBean
    private McpToolsController mcpToolsController;

    private static final String RANDOM_DEPARTURE_ENDPOINT = "/random-departure";
    private static final String FASTEST_ROUTE_ENDPOINT = "/route/fastest";


    @Test
    void shouldReturnDepartureJson_whenServiceReturnsData() throws Exception {
        // given
        LocalDateTime departureTime = LocalDateTime.of(2025, 5, 15, 12, 0, 0);
        RandomDepartureDto dto = RandomDepartureDto.builder()
                .vehicleId("TRAM-123")
                .stopId("STOP-KRA")
                .departureTime(departureTime)
                .build();

        given(randomDepartureService.getRandomDepartureInfo()).willReturn(dto);

        // when & then
        mockMvc.perform(get(RANDOM_DEPARTURE_ENDPOINT)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.vehicleId", is("TRAM-123")))
                .andExpect(jsonPath("$.stopId", is("STOP-KRA")))
                .andExpect(jsonPath("$.departureTime", is("2025-05-15 12:00:00")));
    }


    @Test
    void shouldReturn500_whenServiceThrowsInvalidProtocolBufferException() throws Exception {
        // given
        given(randomDepartureService.getRandomDepartureInfo()).willThrow(new InvalidProtocolBufferException("Data parsing error"));

        // when & then
        mockMvc.perform(get(RANDOM_DEPARTURE_ENDPOINT))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Unexpected server error")));
    }

    @Test
    void shouldReturn500_whenServiceThrowsLogicException() throws Exception {
        // given
        given(randomDepartureService.getRandomDepartureInfo()).willThrow(new NotFoundException("No trip updates available"));

        // when & then
        mockMvc.perform(get(RANDOM_DEPARTURE_ENDPOINT))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No trip updates available")));
    }

    @Test
    void shouldReturn500_whenServiceThrowsExternalServiceException() throws Exception {
        // given
        given(randomDepartureService.getRandomDepartureInfo()).willThrow(
                new ExternalServiceException(
                        "Failed to fetch TripUpdates.pb from external GTFS endpoint",
                        new NullPointerException()
                ));

        // when & then
        mockMvc.perform(get(RANDOM_DEPARTURE_ENDPOINT))
                .andExpect(status().isBadGateway());
    }

    @Test
    void shouldReturnRouteJson_whenServiceReturnsData() throws Exception {
        // given
        RouteSearchResultDto dto = RouteSearchResultDto.builder()
                .startStop("START-A")
                .endStop("END-B")
                .tripId("TRIP-1")
                .routeId("ROUTE-1")
                .routeShortName("10")
                .departureTime(LocalTime.of(9, 0, 0))
                .arrivalTime(LocalTime.of(9, 30, 0))
                .delayInSeconds(60)
                .build();

        given(routeService.findFastestConnection("START-A", "END-B")).willReturn(dto);

        // when & then
        mockMvc.perform(get(FASTEST_ROUTE_ENDPOINT)
                        .param("start", "START-A")
                        .param("end", "END-B")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.startStop", is("START-A")))
                .andExpect(jsonPath("$.endStop", is("END-B")))
                .andExpect(jsonPath("$.routeShortName", is("10")))
                .andExpect(jsonPath("$.departureTime", is("09:00:00")));
    }

    @Test
    void shouldReturnBadRequest_whenMissingParams() throws Exception {
        mockMvc.perform(get(FASTEST_ROUTE_ENDPOINT))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnNotFound_whenServiceThrowsNotFoundException() throws Exception {
        // given
        given(routeService.findFastestConnection("A", "B")).willThrow(new NotFoundException("No route found"));

        // when & then
        mockMvc.perform(get(FASTEST_ROUTE_ENDPOINT)
                        .param("start", "A")
                        .param("end", "B"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No route found")));
    }

    @Test
    void shouldReturnBadGateway_whenServiceThrowsExternalServiceException_forRoute() throws Exception {
        // given
        given(routeService.findFastestConnection("X", "Y")).willThrow(
                new ExternalServiceException("Upstream failure", new RuntimeException())
        );

        // when & then
        mockMvc.perform(get(FASTEST_ROUTE_ENDPOINT)
                        .param("start", "X")
                        .param("end", "Y"))
                .andExpect(status().isBadGateway());
    }
}