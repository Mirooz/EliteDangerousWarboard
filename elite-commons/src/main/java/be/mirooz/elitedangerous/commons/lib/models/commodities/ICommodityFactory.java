package be.mirooz.elitedangerous.commons.lib.models.commodities;

import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.CoreMineralType;

import java.util.Optional;

/**
 * Factory pour créer des objets ICommodity à partir de différents identifiants
 * 
 * Design pattern Factory pour gérer les différents types de commodités :
 * - Core minerals (minéraux de core mining)
 * - Limpets (drones)
 * - Autres commodités (à venir)
 */
public class ICommodityFactory {

    private ICommodityFactory() {}

    /**
     * Crée une commodité à partir de son nom cargo JSON
     * 
     * @param cargoJsonName Le nom cargo JSON (ex: "benitoite", "drones")
     * @return Optional contenant la commodité ou vide si non trouvée
     */
    public static Optional<ICommodity> ofByCargoJson(String cargoJsonName) {
        if (cargoJsonName == null || cargoJsonName.isBlank()) {
            return Optional.empty();
        }
        
        String key = cargoJsonName.toLowerCase().trim();

        // Recherche dans tous les types de commodités
        return searchInCoreMinerals(key)
                .or(() -> searchInLimpets(key))
                .or(Optional::empty);
    }

    /**
     * Recherche dans les core minerals
     */
    private static Optional<ICommodity> searchInCoreMinerals(String key) {
        return CoreMineralType.fromCargoJsonName(key)
                .map(ICommodity.class::cast);
    }

    /**
     * Recherche dans les limpets
     */
    private static Optional<ICommodity> searchInLimpets(String key) {
        return LimpetType.fromCargoJsonName(key)
                .map(ICommodity.class::cast);
    }

}
