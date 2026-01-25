package pl.edu.agh.to.dbresults;

import pl.edu.agh.to.model.StopTime;
import pl.edu.agh.to.model.Trip;

public record FindNextDeparturesForLineResult(
        StopTime stopTime,
        Trip trip
) {}
