package pl.edu.agh.to.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
@JsonPropertyOrder({ "vehicleId", "stopId", "departureTime" })
public class RandomDepartureDto {

    String vehicleId;
    String stopId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime departureTime;
}