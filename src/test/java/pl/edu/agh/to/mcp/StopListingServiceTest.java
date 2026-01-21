package pl.edu.agh.to.mcp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import pl.edu.agh.to.config.McpStopsProperties;
import pl.edu.agh.to.dto.StopNamesSliceDto;
import pl.edu.agh.to.exceptions.BadRequestException;
import pl.edu.agh.to.repository.StopRepository;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StopListingServiceTest {

    @Mock
    private StopRepository stopRepository;

    @Mock
    private McpStopsProperties props;

    @InjectMocks
    private StopListingService service;

    @BeforeEach
    void setUp() {
        lenient().when(props.defaultSize()).thenReturn(10);
        lenient().when(props.maxSize()).thenReturn(100);
    }

    @Test
    void shouldListStopsWithoutQuery() {
        // given
        String lastStop = "Abc";
        int limit = 5;
        List<String> mockStops = List.of("Def", "Ghi");
        Slice<String> slice = new PageImpl<>(mockStops);

        when(stopRepository.findDistinctNamesAfter(eq(lastStop), any(Pageable.class)))
                .thenReturn(slice);

        // when
        StopNamesSliceDto result = service.listStopNames(null, limit, lastStop);

        // then
        assertThat(result.stops()).containsExactly("Def", "Ghi");
        assertThat(result.lastStopName()).isEqualTo("Ghi");
        verify(stopRepository)
                .findDistinctNamesAfter(eq(lastStop), any(Pageable.class));
    }

    @Test
    void shouldListStopsWithPrefixQuery() {
        // given
        String query = "Kra";
        List<String> mockStops = List.of("Kraków Główny");
        Slice<String> slice = new PageImpl<>(mockStops);

        when(stopRepository.findDistinctNamesByPrefixAfter(eq("Kra"), eq(null), any(Pageable.class)))
                .thenReturn(slice);

        // when
        StopNamesSliceDto result = service.listStopNames(query, null, null);

        // then
        assertThat(result.stops()).containsExactly("Kraków Główny");
        verify(stopRepository).findDistinctNamesByPrefixAfter(eq("Kra"), eq(null), any(Pageable.class));
    }

    @Test
    void shouldThrowExceptionWhenLimitIsExceeded() {
        // given
        int limit = 101;

        // when
        Throwable thrown = catchThrowable(() -> service.listStopNames(null, limit, null));

        // then
        assertThat(thrown)
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("limit must be in range");
    }

    @Test
    void shouldThrowExceptionWhenLimitIsZeroOrNegative() {
        // when
        Throwable thrown = catchThrowable(() -> service.listStopNames(null, 0, null));

        //then
        assertThat(thrown).isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldUseDefaultLimitWhenNullProvided() {
        // given
        Slice<String> slice = new PageImpl<>(Collections.emptyList());
        when(stopRepository.findDistinctNamesAfter(any(), any())).thenReturn(slice);

        // when
        service.listStopNames(null, null, null);

        // then
        verify(props).defaultSize();
        verify(stopRepository).findDistinctNamesAfter(eq(null), any(Pageable.class));
    }

    @Test
    void shouldHandleEmptyResultCorrectly() {
        // given
        Slice<String> slice = new PageImpl<>(Collections.emptyList());
        when(stopRepository.findDistinctNamesAfter(any(), any())).thenReturn(slice);

        // when
        StopNamesSliceDto result = service.listStopNames(null, 10, null);

        // then
        assertThat(result.stops()).isEmpty();
        assertThat(result.lastStopName()).isNull();
        assertThat(result.hasNext()).isFalse();
    }
}