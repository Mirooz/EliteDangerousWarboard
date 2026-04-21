package be.mirooz.elitedangerous.dashboard.model.colonisation;

/**
 * Une entrée d’étiquette chantier sur la carte système (vue architecte).
 *
 * @param siteLabel nom affiché (infobulle)
 * @param surfacePort {@code true} si type journal surface ({@code CraterPort}, {@code PlanetaryPort}), sinon orbital
 * @param marketId identifiant station (clic → détail à droite)
 */
public record ColonisationArchitectMapCaptionLine(
        String siteLabel,
        ConstructionStatus status,
        long marketId,
        boolean surfacePort) {
}
