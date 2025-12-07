package pl.edu.agh.to.controller;

import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pl.edu.agh.to.model.RandomDepartureDto;
import pl.edu.agh.to.service.RandomDepartureService;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Integration test for the TransportController using MockMvc. */
@WebMvcTest(TransportController.class)
class TransportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RandomDepartureService service;

    // --- Positive Scenario (Happy Path) ---

    @Test
    void shouldReturnDepartureJson_whenServiceReturnsData() throws Exception {
        // given
        LocalDateTime departureTime = LocalDateTime.of(2025, 5, 15, 12, 0, 0);
        RandomDepartureDto dto = RandomDepartureDto.builder()
                .vehicleId("TRAM-123")
                .stopId("STOP-KRA")
                .departureTime(departureTime)
                .build();

        given(service.getRandomDepartureInfo()).willReturn(dto);

        // when & then
        mockMvc.perform(get("/random-departure")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.vehicleId", is("TRAM-123")))
                .andExpect(jsonPath("$.stopId", is("STOP-KRA")))
                .andExpect(jsonPath("$.departureTime", is("2025-05-15 12:00:00")));
    }

    // --- Negative Scenarios (Exception Handling) ---

    @Test
    void shouldReturn500_whenServiceThrowsInvalidProtocolBufferException() throws Exception {
        // given
        given(service.getRandomDepartureInfo()).willThrow(new InvalidProtocolBufferException("Test exception"));

        // when & then
        mockMvc.perform(get("/random-departure"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Data parsing failed")));
    }

    @Test
    void shouldReturn500_whenServiceThrowsLogicException() throws Exception {
        // given
        given(service.getRandomDepartureInfo()).willThrow(new IllegalStateException("No trip updates available"));

        // when & then
        mockMvc.perform(get("/random-departure"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No trip updates available")));
    }
}