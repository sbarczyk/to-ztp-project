package pl.edu.agh.to.controller;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.edu.agh.to.model.RandomDepartureDto;
import pl.edu.agh.to.service.RandomDepartureService;

/**
 * REST controller handling transport information requests.
 */
@RestController
@RequiredArgsConstructor
public class TransportController {

    private final RandomDepartureService randomDepartureService;

    /**
     * Simple home endpoint for service health check.
     */
    @GetMapping("/")
    public String home() {
        return "Transport service is running!";
    }

    /**
     * Returns random departure information fetched from the GTFS API.
     *
     * @return RandomDepartureDto containing vehicle, stop, and time.
     * @throws InvalidProtocolBufferException if GTFS data is corrupted.
     */
    @GetMapping("/random-departure")
    public RandomDepartureDto randomDeparture() throws InvalidProtocolBufferException {
        return randomDepartureService.getRandomDepartureInfo();
    }

}