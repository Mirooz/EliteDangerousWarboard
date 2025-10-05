package be.mirooz.elitedangerous.dashboard.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Modèle représentant un vaisseau détruit dans Elite Dangerous
 */
@SuperBuilder
public class DestroyedBountyShip extends DestroyedShip {
}
