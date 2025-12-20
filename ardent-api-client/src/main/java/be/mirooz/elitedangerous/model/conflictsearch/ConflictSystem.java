package be.mirooz.elitedangerous.model.conflictsearch;

import lombok.Data;

@Data
public class ConflictSystem {
    private String systemName;
    private int surfaceConflicts;
    private String faction;
    private String opponentFaction;
    private double distanceLy;
}
