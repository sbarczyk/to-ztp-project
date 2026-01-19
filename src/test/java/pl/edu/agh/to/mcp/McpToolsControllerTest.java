package pl.edu.agh.to.mcp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.edu.agh.to.dto.ConnectionsResponseDto;
import pl.edu.agh.to.dto.DeparturesResponseDto;
import pl.edu.agh.to.dto.StopNamesSliceDto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class McpToolsControllerTest {

    @Mock
    private StopListingService stopListingService;
    @Mock
    private ConnectionSearchService connectionSearchService;
    @Mock
    private NextDeparturesService nextDeparturesService;
    @Mock
    private DateTimeParamParser dateTimeParser;

    @InjectMocks
    private McpToolsController controller;

    @Test
    void shouldDelegateListStops() {
        // given
        StopNamesSliceDto expected = new StopNamesSliceDto(List.of("A", "B"), "B", false);
        when(stopListingService.listStopNames("query", 10, "last")).thenReturn(expected);

        // when
        StopNamesSliceDto result = controller.listStops("query", 10, "last");

        // then
        assertThat(result).isSameAs(expected);
        verify(stopListingService).listStopNames("query", 10, "last");
    }

    @Test
    void shouldDelegateFindFastestConnectionsAndParseDates() {
        // given
        String dateStr = "2024-01-01";
        String timeStr = "12:00";
        LocalDate parsedDate = LocalDate.of(2024, 1, 1);
        LocalTime parsedTime = LocalTime.of(12, 0);

        when(dateTimeParser.parseDateOrNull(dateStr)).thenReturn(parsedDate);
        when(dateTimeParser.parseTimeOrNull(timeStr)).thenReturn(parsedTime);

        ConnectionsResponseDto expected = new ConnectionsResponseDto("A", "B", parsedDate, parsedTime, Collections.emptyList());
        when(connectionSearchService.findTop3Connections("A", "B", parsedDate, parsedTime))
                .thenReturn(expected);

        // when
        ConnectionsResponseDto result = controller.findFastestConnections("A", "B", dateStr, timeStr);

        // then
        assertThat(result).isSameAs(expected);
        verify(connectionSearchService).findTop3Connections("A", "B", parsedDate, parsedTime);
    }

    @Test
    void shouldDelegateListNextDeparturesAndParseDates() {
        // given
        String dateStr = "2024-01-01";
        String timeStr = "10:00";
        LocalDate parsedDate = LocalDate.of(2024, 1, 1);
        LocalTime parsedTime = LocalTime.of(10, 0);

        when(dateTimeParser.parseDateOrNull(dateStr)).thenReturn(parsedDate);
        when(dateTimeParser.parseTimeOrNull(timeStr)).thenReturn(parsedTime);

        DeparturesResponseDto expected = new DeparturesResponseDto("Stop", "Line", parsedDate, parsedTime, Collections.emptyList());
        when(nextDeparturesService.getNext5Departures("Stop", "Line", parsedDate, parsedTime))
                .thenReturn(expected);

        // when
        DeparturesResponseDto result = controller.listNextDepartures("Stop", "Line", dateStr, timeStr);

        // then
        assertThat(result).isSameAs(expected);
        verify(nextDeparturesService).getNext5Departures("Stop", "Line", parsedDate, parsedTime);
    }
}