package pl.edu.agh.to.controller;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.edu.agh.to.gtfs.statics.StaticGtfsClient;
import pl.edu.agh.to.gtfs.statics.StaticGtfsService;
import pl.edu.agh.to.model.RandomDepartureDto;
import pl.edu.agh.to.model.RouteSearchResult;
import pl.edu.agh.to.repository.StopTimeRepository;
import pl.edu.agh.to.repository.TripRepository;
import pl.edu.agh.to.service.RandomDepartureService;
import pl.edu.agh.to.service.RouteService;

import java.util.List;
import java.util.Map;


@RestController
@RequiredArgsConstructor
public class TransportController {

    private final RandomDepartureService randomDepartureService;
    private final StaticGtfsService staticGtfsService;
    private final RouteService routeService;

    private final TripRepository tripRepository;
    private final StopTimeRepository stopTimeRepository;

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

//    @GetMapping("/static/extract")
//    public Map<String, List<String>> downloadAndExtractStatic() {
//        var roots = staticGtfsService.downloadAndExtractAll();
//
//        return roots.entrySet().stream()
//                .collect(java.util.stream.Collectors.toMap(
//                        Map.Entry::getKey,
//                        e -> {
//                            try (var stream = java.nio.file.Files.list(e.getValue())) {
//                                return stream
//                                        .map(p -> p.getFileName().toString())
//                                        .sorted()
//                                        .toList();
//                            } catch (java.io.IOException ex) {
//                                throw new IllegalStateException("Failed to list extracted dir", ex);
//                            }
//                        }
//                ));
//    }


    @GetMapping("/route/fastest")
    public RouteSearchResult getFastestRoute(
            @RequestParam String start,
            @RequestParam String end) {
        return routeService.findFastestConnection(start, end);
    }

    @GetMapping("/debug/db")
    public String debugDatabase() {
        long tripCount = tripRepository.count();
        long stopTimeCount = stopTimeRepository.count();

        StringBuilder sb = new StringBuilder();
        sb.append("Liczba Tripów: ").append(tripCount).append("\n");
        sb.append("Liczba StopTimes: ").append(stopTimeCount).append("\n");

        if (tripCount > 0) {
            var t = tripRepository.findAll().get(0);
            sb.append("\nPRZYKŁADOWY TRIP:\n");
            sb.append("ID w bazie: '").append(t.getTripId()).append("'\n");
            sb.append("RouteID: '").append(t.getRouteId()).append("'\n");
            sb.append("ServiceID: '").append(t.getServiceId()).append("'\n");
        }

        if (stopTimeCount > 0) {
            var st = stopTimeRepository.findAll().get(0);
            sb.append("\nPRZYKŁADOWY STOP_TIME:\n");
            sb.append("TripID (klucz obcy): '").append(st.getTripId()).append("'\n");
            sb.append("StopID: '").append(st.getStopId()).append("'\n");
        }

        return sb.toString();
    }

}



