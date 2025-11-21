package be.mirooz.elitedangerous.dashboard.model.exploration;

import be.mirooz.elitedangerous.dashboard.model.registries.exploration.SystemVisitedRegistry;

import java.util.List;

public interface ExplorationData {
    long getTotalEarnings();
    String getStartTimeStamp();
    String getEndTimeStamp();

    List<SystemVisited> getSystemsVisited();
}
