package pl.edu.agh.to.mcp;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import pl.edu.agh.to.config.McpStopsProperties;
import pl.edu.agh.to.exceptions.BadRequestException;
import pl.edu.agh.to.dto.StopNamesSliceDto;
import pl.edu.agh.to.repository.StopRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StopListingService {

    private final StopRepository stopRepository;
    private final McpStopsProperties props;

    public StopNamesSliceDto listStopNames(String query, Integer limit, String lastStopName) {
        int lim = (limit == null) ? props.defaultSize() : limit;

        if (lim < 1 || lim > props.maxSize()) {
            throw new BadRequestException("limit must be in range 1.." + props.maxSize());
        }

        String prefix = StopNormalizer.normalize(query);
        String last = StopNormalizer.normalize(lastStopName);

        var pageable = PageRequest.of(0, lim);

        var slice = (prefix == null)
                ? stopRepository.findDistinctNamesAfter(last, pageable)
                : stopRepository.findDistinctNamesByPrefixAfter(prefix, last, pageable);

        List<String> stops = slice.getContent();
        String newLastStopName = stops.isEmpty() ? null : stops.getLast();

        return new StopNamesSliceDto(stops, newLastStopName, slice.hasNext());
    }
}