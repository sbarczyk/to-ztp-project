package pl.edu.agh.to.client;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.doReturn;

class GtfsClientTest {

    @Test
    void shouldReturnByteArray_givenWebClientReturnsData_whenFetchTripUpdatesAsBytes_thenCorrectBytesReturned() {
        // given
        byte[] expectedBytes = new byte[]{1, 2, 3, 4};

        WebClient webClient = Mockito.mock(WebClient.class);

        WebClient.RequestHeadersUriSpec<?> uriSpec =
                Mockito.mock(WebClient.RequestHeadersUriSpec.class);

        WebClient.RequestHeadersSpec<?> headersSpec =
                Mockito.mock(WebClient.RequestHeadersSpec.class);

        WebClient.ResponseSpec responseSpec =
                Mockito.mock(WebClient.ResponseSpec.class);

        doReturn(uriSpec).when(webClient).get();
        doReturn(headersSpec).when(uriSpec).uri(Mockito.anyString());
        doReturn(responseSpec).when(headersSpec).retrieve();
        doReturn(Mono.just(expectedBytes)).when(responseSpec).bodyToMono(byte[].class);

        GtfsClient client = new GtfsClient(webClient);

        // when
        byte[] result = client.fetchTripUpdatesAsBytes();

        // then
        assertArrayEquals(expectedBytes, result);
    }
}