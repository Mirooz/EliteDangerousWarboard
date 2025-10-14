package be.mirooz.elitedangerous.lib.inara.model.commodities.minerals;

import java.util.Optional;

/**
 * Factory pour créer des objets Mineral à partir de différents identifiants
 */
public class MineralFactory {

    private MineralFactory() {}

    /**
     * Crée un minéral à partir de son nom de minage raffiné
     * @param name Le nom de minage raffiné (ex: "$benitoite_name;")
     * @return Optional contenant le minéral ou vide si non trouvé
     */
    public static Optional<Mineral> fromMiningRefinedName(String name) {
        if (name == null || name.isBlank()) return Optional.empty();

        // Core Minerals
        for (CoreMineralType type : CoreMineralType.values()) {
            if (type.getMiningRefinedName().equalsIgnoreCase(name)) return Optional.of(type);
        }

//        // Surface Minerals
//        for (SurfaceMineralType type : SurfaceMineralType.values()) {
//            assert type.getMiningRefinedName() != null;
//            if (type.getMiningRefinedName().equalsIgnoreCase(name)) return Optional.of(type);
//        }

        return Optional.empty();
    }

    /**
     * Crée un minéral à partir de son nom cargo JSON
     * @param name Le nom cargo JSON (ex: "benitoite")
     * @return Optional contenant le minéral ou vide si non trouvé
     */
    public static Optional<Mineral> fromCargoJsonName(String name) {
        if (name == null || name.isBlank()) return Optional.empty();

        // Core Minerals
        for (CoreMineralType type : CoreMineralType.values()) {
            if (type.getCargoJsonName().equalsIgnoreCase(name)) return Optional.of(type);
        }

        // Surface Minerals
        // for (SurfaceMineralType type : SurfaceMineralType.values()) { ... }

        return Optional.empty();
    }

    /**
     * Crée un minéral à partir de son nom de minéral prospecté
     * @param name Le nom de minéral prospecté (ex: "Benitoite")
     * @return Optional contenant le minéral ou vide si non trouvé
     */
    public static Optional<CoreMineralType> fromCoreMineralName(String name) {
        if (name == null || name.isBlank()) return Optional.empty();

        // Core Minerals
        for (CoreMineralType type : CoreMineralType.values()) {
            if (type.getEdToolName().equalsIgnoreCase(name)) return Optional.of(type);
        }


        return Optional.empty();
    }
}
