package be.mirooz.elitedangerous.commons.lib.models.commodities;

/**
 * Catégorie Inara (libellé normalisé dans {@code ardent-inara-registry.json}).
 */
public enum CommodityCategory {
    CHEMICALS,
    CONSUMER_ITEMS,
    LEGAL_DRUGS,
    FOODS,
    INDUSTRIAL_MATERIALS,
    MACHINERY,
    MEDICINES,
    METALS,
    MINERALS,
    SALVAGE,
    SLAVES,
    TECHNOLOGY,
    TEXTILES,
    WASTE,
    WEAPONS,
    NON_MARKETABLE,
    UNKNOWN;

    public static CommodityCategory fromRegistryValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return UNKNOWN;
        }
        try {
            return valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
