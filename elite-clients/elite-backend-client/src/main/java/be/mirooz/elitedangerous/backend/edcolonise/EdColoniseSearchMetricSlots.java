package be.mirooz.elitedangerous.backend.edcolonise;

/**
 * Paires min/max (indices tableaux API) pour chaque filtre « corps » ED Colonise.
 */
public final class EdColoniseSearchMetricSlots {

    public static final EdColoniseStarSystemSearchQuery.BodyMinMaxSlot AMMONIA =
            new EdColoniseStarSystemSearchQuery.BodyMinMaxSlot(0, 0, "ammoniaWorlds");
    public static final EdColoniseStarSystemSearchQuery.BodyMinMaxSlot BLACK_HOLES =
            new EdColoniseStarSystemSearchQuery.BodyMinMaxSlot(1, 1, "blackHoles");
    public static final EdColoniseStarSystemSearchQuery.BodyMinMaxSlot EARTH_LIKES =
            new EdColoniseStarSystemSearchQuery.BodyMinMaxSlot(2, 3, "earthLikes");
    public static final EdColoniseStarSystemSearchQuery.BodyMinMaxSlot GAS_GIANTS =
            new EdColoniseStarSystemSearchQuery.BodyMinMaxSlot(3, 4, "gasGiants");
    public static final EdColoniseStarSystemSearchQuery.BodyMinMaxSlot GEOLOGICALS =
            new EdColoniseStarSystemSearchQuery.BodyMinMaxSlot(4, 5, "geologicals");
    public static final EdColoniseStarSystemSearchQuery.BodyMinMaxSlot HIGH_METAL_CONTENTS =
            new EdColoniseStarSystemSearchQuery.BodyMinMaxSlot(5, 6, "highMetalContents");
    public static final EdColoniseStarSystemSearchQuery.BodyMinMaxSlot ICES =
            new EdColoniseStarSystemSearchQuery.BodyMinMaxSlot(6, 7, "ices");
    public static final EdColoniseStarSystemSearchQuery.BodyMinMaxSlot LANDABLES =
            new EdColoniseStarSystemSearchQuery.BodyMinMaxSlot(7, 8, "landables");
    public static final EdColoniseStarSystemSearchQuery.BodyMinMaxSlot METAL_RICHES =
            new EdColoniseStarSystemSearchQuery.BodyMinMaxSlot(8, 9, "metalRiches");
    public static final EdColoniseStarSystemSearchQuery.BodyMinMaxSlot NEUTRON_STARS =
            new EdColoniseStarSystemSearchQuery.BodyMinMaxSlot(9, 10, "neutronStars");
    public static final EdColoniseStarSystemSearchQuery.BodyMinMaxSlot ORGANICS =
            new EdColoniseStarSystemSearchQuery.BodyMinMaxSlot(10, 11, "organics");
    public static final EdColoniseStarSystemSearchQuery.BodyMinMaxSlot OTHER_STARS =
            new EdColoniseStarSystemSearchQuery.BodyMinMaxSlot(11, 12, "otherStars");
    public static final EdColoniseStarSystemSearchQuery.BodyMinMaxSlot RINGS =
            new EdColoniseStarSystemSearchQuery.BodyMinMaxSlot(12, 13, "rings");
    public static final EdColoniseStarSystemSearchQuery.BodyMinMaxSlot ROCKS =
            new EdColoniseStarSystemSearchQuery.BodyMinMaxSlot(13, 14, "rocks");
    public static final EdColoniseStarSystemSearchQuery.BodyMinMaxSlot ROCKY_ICES =
            new EdColoniseStarSystemSearchQuery.BodyMinMaxSlot(14, 15, "rockyIces");
    public static final EdColoniseStarSystemSearchQuery.BodyMinMaxSlot WALKABLES =
            new EdColoniseStarSystemSearchQuery.BodyMinMaxSlot(15, 16, "walkables");
    public static final EdColoniseStarSystemSearchQuery.BodyMinMaxSlot WATER_WORLDS =
            new EdColoniseStarSystemSearchQuery.BodyMinMaxSlot(16, 17, "waterWorlds");
    public static final EdColoniseStarSystemSearchQuery.BodyMinMaxSlot WHITE_DWARVES =
            new EdColoniseStarSystemSearchQuery.BodyMinMaxSlot(17, 18, "whiteDwarves");

    /** Ordre d’affichage colonne gauche puis droite (UI). */
    public static final EdColoniseStarSystemSearchQuery.BodyMinMaxSlot[] ALL = {
            LANDABLES, BLACK_HOLES, WHITE_DWARVES, EARTH_LIKES, AMMONIA, HIGH_METAL_CONTENTS,
            ROCKY_ICES, ICES, GEOLOGICALS,
            WALKABLES, NEUTRON_STARS, OTHER_STARS, WATER_WORLDS, GAS_GIANTS, METAL_RICHES,
            ROCKS, RINGS, ORGANICS
    };

    /**
     * Filtres corps pour la fenêtre « plus de filtres » : min landables / min rings sont sur la ligne principale.
     */
    public static final EdColoniseStarSystemSearchQuery.BodyMinMaxSlot[] ALL_EXCEPT_LANDABLES_RINGS = {
            BLACK_HOLES, WHITE_DWARVES, EARTH_LIKES, AMMONIA, HIGH_METAL_CONTENTS,
            ROCKY_ICES, ICES, GEOLOGICALS,
            WALKABLES, NEUTRON_STARS, OTHER_STARS, WATER_WORLDS, GAS_GIANTS, METAL_RICHES,
            ROCKS, ORGANICS
    };

    private EdColoniseSearchMetricSlots() {
    }
}
