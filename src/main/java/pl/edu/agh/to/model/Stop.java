package pl.edu.agh.to.model;

import jakarta.persistence.*;
import lombok.*;

@Entity // <-- To jest teraz tabela w bazie
@Table(name = "stops")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Stop {
    @Id
    private String id; // stop_id (klucz główny)

    private String name;
    private double lat;
    private double lon;
}