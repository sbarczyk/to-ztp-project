package pl.edu.agh.to.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TransportController {

    @GetMapping("/")
    public String home() {
        return "Transport service is running!";
    }


}