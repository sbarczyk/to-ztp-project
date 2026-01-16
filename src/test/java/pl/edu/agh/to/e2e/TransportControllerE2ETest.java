package pl.edu.agh.to.e2e;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.edu.agh.to.exceptions.ApiError;
import pl.edu.agh.to.model.RandomDepartureDto;
import pl.edu.agh.to.model.RouteSearchResultDto;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class TransportControllerE2ETest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void randomDeparture_shouldReturnCorrectDataFormat() {
        webTestClient.get()
                .uri("/random-departure")
                .exchange()
                .expectStatus().isOk()
                .expectBody(RandomDepartureDto.class);
    }

    @Test
    void getFastestRoute_shouldReturnCorrectConnectionData_whenConnectionExists() {
        webTestClient.get()
                .uri("/route/fastest?start=Kawiory&end=Czarnowiejska")
                .exchange()
                .expectStatus().isOk()
                .expectBody(RouteSearchResultDto.class);
    }

    @Test
    void getFastestRoute_shouldReturnNotFound_whenConnectionDoesntExist() {
        webTestClient.get()
                .uri("/route/fastest?start=Bronowice&end=Salwator")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ApiError.class);
    }

    @Test
    void getFastestRoute_shouldReturnBadRequest_whenParametersAreMissing() {
        webTestClient.get()
                .uri("/route/fastest")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ApiError.class);

        webTestClient.get()
                .uri("/route/fastest?start=Kawiory")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ApiError.class);

        webTestClient.get()
                .uri("/route/fastest?end=Czarnowiejska")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ApiError.class);
    }
}
