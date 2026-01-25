package pl.edu.agh.to.mcp;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import pl.edu.agh.to.dto.ConnectionsResponseDto;
import pl.edu.agh.to.dto.DeparturesResponseDto;
import pl.edu.agh.to.dto.StopNamesSliceDto;

@Service
@RequiredArgsConstructor
public class McpToolsController {

    private final StopListingService stopListingService;
    private final ConnectionSearchService connectionSearchService;
    private final NextDeparturesService nextDeparturesService;

    @Tool(description = "List stop names in Krakow (unique names) using cursor-based pagination.")
    public StopNamesSliceDto listStops(
            @ToolParam(description = "Optional stop name prefix filter.", required = false)
            String query,
            @ToolParam(description = "Max number of names to return.", required = false)
            Integer limit,
            @ToolParam(description = "Continuation marker from previous response.", required = false)
            String lastStopName
    ) {
        return stopListingService.listStopNames(query, limit, lastStopName);
    }

    @Tool(description = "Find up to 3 direct connections that depart soonest between two stops, including delays.")
    public ConnectionsResponseDto findFastestConnections(
            @ToolParam(description = "Start stop name.") String fromStop,
            @ToolParam(description = "Destination stop name.") String toStop,
            @ToolParam(description = "Optional date (yyyy-MM-dd).", required = false) String date,
            @ToolParam(description = "Optional time (HH:mm or HH:mm:ss).", required = false) String time
    ) {
        return connectionSearchService.findTop3Connections(
                fromStop,
                toStop,
                DateTimeParamParser.parseDateOrNull(date),
                DateTimeParamParser.parseTimeOrNull(time)
        );
    }

    @Tool(description = "Show next 5 departures for a given stop and line, including delays.")
    public DeparturesResponseDto listNextDepartures(
            @ToolParam(description = "Stop name.") String stopName,
            @ToolParam(description = "Line number.") String lineNumber,
            @ToolParam(description = "Optional date (yyyy-MM-dd).", required = false) String date,
            @ToolParam(description = "Optional time (HH:mm or HH:mm:ss).", required = false) String time
    ) {
        return nextDeparturesService.getNext5Departures(
                stopName,
                lineNumber,
                DateTimeParamParser.parseDateOrNull(date),
                DateTimeParamParser.parseTimeOrNull(time)
        );
    }
}