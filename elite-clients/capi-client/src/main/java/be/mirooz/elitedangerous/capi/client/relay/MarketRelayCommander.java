package be.mirooz.elitedangerous.capi.client.relay;

/**
 * Données commandant pour EDDN / Inara (fournies par l'app, ex. depuis {@code CommanderStatus}).
 */
public record MarketRelayCommander(
        String fid,
        String commanderName,
        String currentStarSystem,
        String currentStationName,
        String gameVersion,
        String gameBuild,
        Boolean horizons,
        Boolean odyssey
) {
}
