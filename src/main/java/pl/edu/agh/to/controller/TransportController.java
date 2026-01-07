package pl.edu.agh.to.controller;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.edu.agh.to.model.RandomDepartureDto;
import pl.edu.agh.to.model.RouteSearchResult;
import pl.edu.agh.to.service.RandomDepartureService;
import pl.edu.agh.to.service.RouteService;


@RestController
@RequiredArgsConstructor
public class TransportController {

    private final RandomDepartureService randomDepartureService;
    private final RouteService routeService;

    @GetMapping("/")
    public String home() {
        return "Transport service is running!";
    }

    /**
     * Returns a random vehicle departure based on current real-time data.
     */
    @GetMapping("/random-departure")
    public RandomDepartureDto randomDeparture() throws InvalidProtocolBufferException {
        return randomDepartureService.getRandomDepartureInfo();
    }

    /**
     * Finds the fastest direct connection between two stops.
     * * @param start Name of the starting stop.
     * @param end Name of the destination stop.
     * @return Result containing connection details and estimated arrival time.
     */
    @GetMapping("/route/fastest")
    public RouteSearchResult getFastestRoute(
            @RequestParam String start,
            @RequestParam String end) {
        return routeService.findFastestConnection(start, end);
    }
}