package pl.edu.agh.to.controller;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.edu.agh.to.gtfs.statics.StaticGtfsClient;
import pl.edu.agh.to.gtfs.statics.StaticGtfsService;
import pl.edu.agh.to.model.RandomDepartureDto;
import pl.edu.agh.to.service.RandomDepartureService;

import java.util.List;
import java.util.Map;


@RestController
@RequiredArgsConstructor
public class TransportController {

    private final RandomDepartureService randomDepartureService;
    private final StaticGtfsService staticGtfsService;

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

    @GetMapping("/static/extract")
    public Map<String, List<String>> downloadAndExtractStatic() {
        var roots = staticGtfsService.downloadAndExtractAll();

        return roots.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            try (var stream = java.nio.file.Files.list(e.getValue())) {
                                return stream
                                        .map(p -> p.getFileName().toString())
                                        .sorted()
                                        .toList();
                            } catch (java.io.IOException ex) {
                                throw new IllegalStateException("Failed to list extracted dir", ex);
                            }
                        }
                ));
    }

}