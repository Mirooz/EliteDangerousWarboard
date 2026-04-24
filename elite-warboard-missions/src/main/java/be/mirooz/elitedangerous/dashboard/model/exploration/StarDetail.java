package be.mirooz.elitedangerous.dashboard.model.exploration;

import be.mirooz.elitedangerous.biologic.StarType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Détails d'une étoile scannée dans Elite Dangerous.
 *
 * <p>{@link NoArgsConstructor} requis pour la désérialisation Jackson (cf. {@link ACelesteBody}).</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
public class StarDetail extends ACelesteBody {

    private String starTypeString;

    private StarType starType;
    private double stellarMass;
    @Override
    public long computeBodyValue() {

        boolean firstDiscover = !wasDiscovered;
        boolean isFleetCarrierSale = false;

        // Formule officielle (communauté reverse-engineered)
        double value = starType.getKValue() + (stellarMass * starType.getKValue() / 66.25);
        if (firstDiscover) {
            value *= 2.6;
        }

        if (isFleetCarrierSale) {
            value *= 0.75;
        }

        return (int) Math.round(value);
    }
}
