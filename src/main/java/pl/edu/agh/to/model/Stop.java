package pl.edu.agh.to.model;

import lombok.Value;

@Value
public class Stop {
    String id;
    String name;
    double lat;
    double lon;
}