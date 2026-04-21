package be.mirooz.elitedangerous.commons.lib.models.commodities;

import java.util.Locale;

/**
 * Clé de fusion chantier / fleet (ressources construction ↔ stock carrier).
 */
public final class ColonisationCommodityKeys {

    private ColonisationCommodityKeys() {}

    /**
     * Normalise un libellé ou identifiant brut pour comparaison (minuscules, sans espaces ni underscores,
     * troncature des tokens de localisation type {@code $foo_name;}).
     */
    public static String normalizeMergeKey(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim().toLowerCase(Locale.ROOT);
        if (s.startsWith("$")) {
            s = s.substring(1);
        }
        if (s.endsWith(";")) {
            s = s.substring(0, s.length() - 1);
        }
        int nameIdx = s.indexOf("_name");
        if (nameIdx > 0) {
            s = s.substring(0, nameIdx);
        }
        return s.replace(" ", "").replace("_", "");
    }

    /** Clé de fusion dérivée d’une commodité résolue (nom cargo JSON). */
    public static String mergeKey(ICommodity c) {
        if (c == null) {
            return "";
        }
        return normalizeMergeKey(c.getCargoJsonName());
    }
}
