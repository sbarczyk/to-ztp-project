package pl.edu.agh.to.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import pl.edu.agh.to.model.StopNamesSliceDto;

@Service
@RequiredArgsConstructor
public class AIToolsService {

    private final StopListingService stopListingService;

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
}