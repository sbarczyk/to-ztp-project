package pl.edu.agh.to.model;

import java.util.List;

public record StopNamesSliceDto(
        List<String> stops,
        String lastStopName,
        boolean hasNext
) { }