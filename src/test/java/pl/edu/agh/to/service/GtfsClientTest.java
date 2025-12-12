package pl.edu.agh.to.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GtfsClientTest {
    @Mock
    private WebClient webClient;

    @SuppressWarnings("rawtypes") // because thenReturn can't match wild type <?>
    @Mock
    private WebClient.RequestHeadersSpec headersSpec;

    @SuppressWarnings("rawtypes")
    @Mock
    private WebClient.RequestHeadersUriSpec uriSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private GtfsClient gtfsClient;

    @Test
    void shouldReturnByteArray_givenWebClientReturnsData_whenFetchTripUpdatesAsBytes_thenCorrectBytesReturned() {
        // given
        byte[] expectedBytes = new byte[]{1, 2, 3, 4};

        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(byte[].class)).thenReturn(Mono.just(expectedBytes));

        // when
        byte[] result = gtfsClient.fetchTripUpdatesAsBytes();

        // then
        assertArrayEquals(expectedBytes, result);
    }
}