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

    @GetMapping("/random-departure")
    public RandomDepartureDto randomDeparture() throws InvalidProtocolBufferException {
        return randomDepartureService.getRandomDepartureInfo();
    }

    @GetMapping("/route/fastest")
    public RouteSearchResult getFastestRoute(@RequestParam String start, @RequestParam String end) {
        return routeService.findFastestConnection(start, end);
    }
}