package pl.edu.agh.to.controller;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import pl.edu.agh.to.model.RandomDepartureDto;
import pl.edu.agh.to.service.RandomDepartureService;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransportControllerTest {

    @Test
    void shouldReturnDto_givenServiceReturnsDto_thenControllerReturnsSameDto() throws Exception {

        // given
        var service = Mockito.mock(RandomDepartureService.class);

        var dto = RandomDepartureDto.builder()
                .vehicleId("A:57")
                .stopId("2048494")
                .departureTime(LocalDateTime.now())
                .build();

        Mockito.when(service.getRandomDepartureInfo()).thenReturn(dto);

        var controller = new TransportController(service);

        // when
        RandomDepartureDto result = controller.randomDeparture();

        // then
        assertEquals(dto, result);
    }

    @Test
    void shouldReturnHomeMessage_whenHomeEndpointCalled_thenCorrectMessageReturned() {

        // given
        var service = Mockito.mock(RandomDepartureService.class);
        var controller = new TransportController(service);

        // when
        String result = controller.home();

        // then
        assertEquals("Transport service is running!", result);
    }
}