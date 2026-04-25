package be.mirooz.elitedangerous.commons.lib.models.commodities;

import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.MineralType;
import java.util.Optional;

/**
 * Factory pour créer des objets ICommodity à partir de différents identifiants
 * 
 * Design pattern Factory pour gérer les différents types de commodités :
 * - Core minerals (minéraux de core mining)
 * - Limpets (drones)
 * - Registre Ardent/Inara ({@link CommodityLoader})
 */
public class ICommodityFactory {

    private ICommodityFactory() {}

    /**
     * Crée une commodité à partir de son nom cargo JSON
     * <p>
     * Accepte les variantes usuelles du journal / {@code Cargo.json} : trim, casse,
     * puis une seconde passe « clé alphanumérique » (sans espaces, tirets, underscores)
     * pour coller aux {@code cargoJsonName} du registre (ex. {@code meta-alloys} → {@code metaalloys}).
     *
     * @param cargoJsonName Le nom cargo JSON (ex: "benitoite", "drones", "Gold")
     * @return Optional contenant la commodité ou vide si non trouvée
     */
    public static Optional<ICommodity> ofByCargoJson(String cargoJsonName) {
        if (cargoJsonName == null || cargoJsonName.isBlank()) {
            return Optional.empty();
        }

        String key = cargoJsonName.toLowerCase().trim();
        Optional<ICommodity> base = CommodityLoader.findByCargoJsonName(key);

        return searchInCoreMinerals(key)
                .or(() -> base)
                .or(() -> searchInLimpets(key))
                .map(result -> {
                    base.map(ICommodity::getInaraCommodityCategory)
                            .ifPresent(result::setInaraCommodityCategory);
                    return result;
                });
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
                .or(() -> CommodityLoader.findByInaraId(inaraId));
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

    /**
     * Résout une commodité à partir de champs persistés (JSON) : d’abord
     * {@link #ofByCargoJson} / {@link #ofByInaraId} (registre {@link CommodityLoader},
     * minéraux, limpets), puis le même repli qu’en runtime via {@link CarrierCommodityResolver}
     * pour les commodités non référencées.
     *
     * @param inaraName optionnel, utilisé en secours (anciens snapshots ou libellé) ; un ID Inara
     *                  entièrement numérique est testé via {@link #ofByInaraId}.
     * @return jamais {@code null} si au moins une chaîne non vide est fournie
     */
    public static ICommodity fromPersisted(String cargoJsonName, String inaraName) {
        String c = cargoJsonName == null || cargoJsonName.isBlank() ? null : cargoJsonName;
        String i = inaraName == null || inaraName.isBlank() ? null : inaraName;
        if (c != null) {
            Optional<ICommodity> o = ofByCargoJson(c);
            if (o.isPresent()) {
                return o.get();
            }
        }
        if (i != null) {
            String trimmed = i.trim();
            if (!trimmed.isEmpty() && trimmed.chars().allMatch(ch -> ch >= '0' && ch <= '9')) {
                Optional<ICommodity> o = ofByInaraId(trimmed);
                if (o.isPresent()) {
                    return o.get();
                }
            }
            Optional<ICommodity> o2 = ofByCargoJson(trimmed);
            if (o2.isPresent()) {
                return o2.get();
            }
        }
        if (c != null || i != null) {
            return CarrierCommodityResolver.resolve(
                    c != null ? c : "",
                    i != null ? i : "");
        }
        return null;
    }

}
