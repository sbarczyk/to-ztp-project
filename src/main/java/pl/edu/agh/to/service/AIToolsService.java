package pl.edu.agh.to.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import pl.edu.agh.to.gtfs.realtime.GtfsClient;

@Service
@RequiredArgsConstructor
public class AIToolsService {

    private final GtfsClient gtfsClient;

    @Tool(description = "Get all public transport stops in Krakow")
    public String getAllStops() {
        return "Kawiory, Chopina i Czarnowiejska";
    }
}
