package be.mirooz.elitedangerous.commons.lib.models.commodities;

import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.MineralType;
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
        return MineralType.fromCargoJsonName(key)
                .map(ICommodity.class::cast);
    }

    /**
     * Recherche dans les limpets
     */
    private static Optional<ICommodity> searchInLimpets(String key) {
        return LimpetType.fromCargoJsonName(key)
                .map(ICommodity.class::cast);
    }

    /**
     * Crée une commodité à partir de son ID Inara
     * 
     * @param inaraId L'ID Inara de la commodité (ex: "81", "10249")
     * @return Optional contenant la commodité ou vide si non trouvée
     */
    public static Optional<ICommodity> ofByInaraId(String inaraId) {
        if (inaraId == null || inaraId.isBlank()) {
            return Optional.empty();
        }
        
        // Recherche dans tous les types de commodités par ID Inara
        return searchInCoreMineralsByInaraId(inaraId)
                .or(() -> searchInLimpetsByInaraId(inaraId))
                .or(Optional::empty);
    }

    /**
     * Recherche dans les core minerals par ID Inara
     */
    private static Optional<ICommodity> searchInCoreMineralsByInaraId(String inaraId) {
        return MineralType.fromInaraId(inaraId)
                .map(ICommodity.class::cast);
    }

    /**
     * Recherche dans les limpets par ID Inara
     */
    private static Optional<ICommodity> searchInLimpetsByInaraId(String inaraId) {
        // Les limpets n'ont pas d'ID Inara, donc retourner vide
        return Optional.empty();
    }

}
