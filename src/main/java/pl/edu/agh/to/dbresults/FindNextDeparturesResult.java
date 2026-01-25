package pl.edu.agh.to.dbresults;

import pl.edu.agh.to.model.Route;
import pl.edu.agh.to.model.StopTime;
import pl.edu.agh.to.model.Trip;

public record FindNextDeparturesResult(
        StopTime stopTimeA,
        StopTime stopTimeB,
        Trip trip,
        Route route
) {}
