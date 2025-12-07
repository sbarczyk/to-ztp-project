package pl.edu.agh.to.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.edu.agh.to.service.GTFSService;

@RestController
@RequiredArgsConstructor
public class TransportController {

    private final GTFSService gtfsService;

    @GetMapping("/")
    public String home() {
        return "Transport service is running!";
    }

    // Żeby zobaczyć czy działa
    // todo: po zrobieniu unit testa należy wywalić
    @GetMapping("/test")
    public String test() {
        return gtfsService.getTestTripString();
    }
}