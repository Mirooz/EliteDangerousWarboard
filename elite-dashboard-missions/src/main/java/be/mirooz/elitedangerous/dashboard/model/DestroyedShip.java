package be.mirooz.elitedangerous.dashboard.model;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Modèle représentant un vaisseau détruit dans Elite Dangerous
 */

@SuperBuilder
@Data
public abstract class DestroyedShip {
    private String shipName;
    private String pilotName;
    private String faction;
    private String bountyFaction;
    private List<Reward> rewards;
    private int totalBountyReward;
    private LocalDateTime destroyedTime;

}
