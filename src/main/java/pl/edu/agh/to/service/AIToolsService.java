package pl.edu.agh.to.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import pl.edu.agh.to.model.ConnectionDto;
import pl.edu.agh.to.model.StopNamesSliceDto;

import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AIToolsService {

    private final StopListingService stopListingService;
    private final McpRouteService mcpRouteService;

    @Tool(description = "List unique stop names in Krakow using cursor-based pagination. Optional prefix filter by stop name.")
    public StopNamesSliceDto getAllStops(
            @ToolParam(description = "Optional stop name prefix filter (case-insensitive). Example: \"Krak\".", required = false)
            String query,
            @ToolParam(description = "Max number of results to return (1..max). Default from config.", required = false)
            Integer limit,
            @ToolParam(description = "Last stop name from previous response; pass it to get the next batch.", required = false)
            String lastStopName
    ) {
        return stopListingService.listStopNames(query, limit, lastStopName);
    }

    @Tool(description = "Find top 3 fastest connections between two stops considering delays.")
    public List<ConnectionDto> findFastestConnections(
            @ToolParam(description = "Name of the start stop.") String startStop,
            @ToolParam(description = "Name of the destination stop.") String endStop,
            @ToolParam(description = "Optional departure time (HH:mm). Default is now.", required = false) String time
    ) {
        LocalTime departureTime = (time != null) ? LocalTime.parse(time) : LocalTime.now();
        return mcpRouteService.findTop3Connections(startStop, endStop, departureTime);
    }
}