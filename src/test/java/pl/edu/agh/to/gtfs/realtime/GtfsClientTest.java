package pl.edu.agh.to.gtfs.realtime;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.transit.realtime.GtfsRealtime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import pl.edu.agh.to.exceptions.ExternalServiceException;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GtfsClientTest {
    @Mock
    private WebClient webClient;

    @Mock
    private GtfsParser gtfsParser;

    @SuppressWarnings("rawtypes")
    @Mock
    private WebClient.RequestHeadersSpec headersSpec;

    @SuppressWarnings("rawtypes")
    @Mock
    private WebClient.RequestHeadersUriSpec uriSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private GtfsRealtime.TripUpdate tripUpdate;

    @InjectMocks
    private GtfsClient gtfsClient;

    @Test
    void givenWebClientReturnsData_whenFetchTripUpdates_thenCorrectUpdateReturned() throws InvalidProtocolBufferException {
        // given
        byte[] someBytes = new byte[]{1, 2, 3, 4};
        List<GtfsRealtime.TripUpdate> updates = List.of(tripUpdate);

        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(byte[].class)).thenReturn(Mono.just(someBytes));
        when(gtfsParser.parseTripUpdates(someBytes)).thenReturn(updates);

        // when
        List<GtfsRealtime.TripUpdate> result = gtfsClient.fetchTripUpdates();

        // then
        assertEquals(updates, result);
    }

    @Test
    void shouldThrowExternalServiceException_whenFetchedTripUpdatesIsNull() {
        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(byte[].class)).thenReturn(null);

        // when
        Throwable thrown = catchThrowable(() -> gtfsClient.fetchTripUpdates());

        // then
        assertThat(thrown).isInstanceOf(ExternalServiceException.class);
    }
}