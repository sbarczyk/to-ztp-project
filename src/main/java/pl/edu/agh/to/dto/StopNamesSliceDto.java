package pl.edu.agh.to.dto;

import java.util.List;

public record StopNamesSliceDto(
        List<String> stops,
        String lastStopName,
        boolean hasNext
) { }