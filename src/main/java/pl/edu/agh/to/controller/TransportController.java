package pl.edu.agh.to.controller;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.edu.agh.to.model.RandomDepartureDto;
import pl.edu.agh.to.service.RandomDepartureService;

@RestController
@RequiredArgsConstructor
public class TransportController {

    private final RandomDepartureService randomDepartureService;

    @GetMapping("/")
    public String home() {
        return "Transport service is running!";
    }

    @GetMapping("/random-departure")
    public RandomDepartureDto randomDeparture() throws InvalidProtocolBufferException {
        return randomDepartureService.getRandomDepartureInfo();
    }

}