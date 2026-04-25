package be.mirooz.elitedangerous.dashboard.model.ships;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Ligne d'un vaisseau détruit (bounty ou bond) — un seul modèle ;
 * le discriminant est {@link #kind} (champ JSON {@code "type"}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DestroyedShip {

    @JsonProperty("type")
    private DestroyedShipKind kind;

    private String shipName;
    private String pilotName;
    private String faction;
    private String bountyFaction;
    private List<Reward> rewards;
    private int totalBountyReward;
    private LocalDateTime destroyedTime;
}
