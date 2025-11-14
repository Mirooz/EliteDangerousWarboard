package be.mirooz.elitedangerous.dashboard.model.exploration;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * Détails d'une étoile scannée dans Elite Dangerous.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class StarDetail extends AbstractCelesteBody {

    private String starType;
}
