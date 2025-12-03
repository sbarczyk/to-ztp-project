package pl.edu.agh.to.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransportController {

    @GetMapping("/")
    public String home() {
        return "Transport service is running!";
    }
}