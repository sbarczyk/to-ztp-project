package pl.edu.agh.to.e2e;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.google.transit.realtime.GtfsRealtime;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import pl.edu.agh.to.model.RandomDepartureDto;

import java.time.Instant;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ToZtpApplicationE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private static WireMockServer wireMockServer;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("ztp.gtfs.url", wireMockServer::baseUrl);
    }

    @Test
    void shouldReturnDepartureInfo_whenExternalGtfsIsAvailable() {
        // given
        long now = Instant.now().getEpochSecond();
        byte[] validGtfsData = createGtfsProtobufData("BUS-E2E", "STOP-E2E", now, true);

        wireMockServer.stubFor(get(urlEqualTo("/TripUpdates.pb"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/x-protobuf")
                        .withBody(validGtfsData)));

        // when
        ResponseEntity<RandomDepartureDto> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/random-departure",
                RandomDepartureDto.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getVehicleId()).isEqualTo("BUS-E2E");
        assertThat(response.getBody().getStopId()).isEqualTo("STOP-E2E");
    }

    @Test
    void shouldReturnError500_whenExternalGtfsIsDown() {
        // given
        wireMockServer.stubFor(get(urlEqualTo("/TripUpdates.pb"))
                .willReturn(aResponse().withStatus(500)));

        // when
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/random-departure",
                String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void shouldReturnError500_whenGtfsDataContainsNoTrips() {
        // given
        byte[] emptyTripsData = createGtfsProtobufData("X", "Y", 0, false);

        wireMockServer.stubFor(get(urlEqualTo("/TripUpdates.pb"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/x-protobuf")
                        .withBody(emptyTripsData)));

        // when
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/random-departure",
                String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void shouldReturnError500_whenGtfsDataIsCorrupted() {
        // given
        byte[] corruptedData = new byte[]{1, 2, 3, 4, 5};

        wireMockServer.stubFor(get(urlEqualTo("/TripUpdates.pb"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/x-protobuf")
                        .withBody(corruptedData)));

        // when
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/random-departure",
                String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private byte[] createGtfsProtobufData(String vehicleId, String stopId, long time, boolean includeTrip) {
        GtfsRealtime.FeedMessage.Builder feedBuilder = GtfsRealtime.FeedMessage.newBuilder()
                .setHeader(GtfsRealtime.FeedHeader.newBuilder()
                        .setGtfsRealtimeVersion("2.0")
                        .setIncrementality(GtfsRealtime.FeedHeader.Incrementality.FULL_DATASET)
                        .setTimestamp(time)
                        .build());

        if (includeTrip) {
            GtfsRealtime.TripUpdate tripUpdate = GtfsRealtime.TripUpdate.newBuilder()
                    .setTrip(GtfsRealtime.TripDescriptor.newBuilder().setTripId("T1").build())
                    .setVehicle(GtfsRealtime.VehicleDescriptor.newBuilder().setId(vehicleId).build())
                    .addStopTimeUpdate(GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder()
                            .setStopId(stopId)
                            .setDeparture(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setTime(time).build())
                            .build())
                    .build();

            GtfsRealtime.FeedEntity entity = GtfsRealtime.FeedEntity.newBuilder()
                    .setId("E1")
                    .setTripUpdate(tripUpdate)
                    .build();

            feedBuilder.addEntity(entity);
        }

        return feedBuilder.build().toByteArray();
    }
}