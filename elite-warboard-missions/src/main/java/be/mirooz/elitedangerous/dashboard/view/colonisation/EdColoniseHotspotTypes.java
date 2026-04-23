package be.mirooz.elitedangerous.dashboard.view.colonisation;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Types de hotspots ED Colonise (affichage avec espaces ; requête API sans espaces, séparés par des virgules).
 */
public final class EdColoniseHotspotTypes {

    /** Ordre alphabétique croissant (libellés affichés). */
    public static final List<String> ALL_SORTED = List.of(
            "Alexandrite",
            "Bauxite",
            "Benitoite",
            "Bromellite",
            "Cobalt",
            "Grandidierite",
            "Hydrogen Peroxide",
            "Indite",
            "Lepidolite",
            "Liquid Oxygen",
            "Lithium Hydroxide",
            "Low Temperature Diamond",
            "Methane Clathrate",
            "Methanol Monohydrate Crystals",
            "Monazite",
            "Musgravite",
            "Opal",
            "Painite",
            "Platinum",
            "Rhodplumsite",
            "Rutile",
            "Samarium",
            "Serendibite",
            "Tritium",
            "Uraninite",
            "Water"
    );

    private EdColoniseHotspotTypes() {
    }

    public static String normalizeKey(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    /**
     * Chaîne API : noms sans espaces, tri alphabétique sur la forme compacte, séparés par des virgules sans espace.
     */
    public static String serializeForApi(Collection<String> selectedDisplayNames) {
        if (selectedDisplayNames == null || selectedDisplayNames.isEmpty()) {
            return "";
        }
        List<String> compact = selectedDisplayNames.stream()
                .map(s -> s == null ? "" : s.replaceAll("\\s+", ""))
                .filter(s -> !s.isEmpty())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        return String.join(",", compact);
    }

    /**
     * Interprète une valeur déjà stockée (virgules, avec ou sans espaces dans les noms) en ensemble de libellés affichables.
     */
    public static Set<String> parseCsvToDisplayNames(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        Set<String> out = new LinkedHashSet<>();
        for (String part : csv.split(",")) {
            String token = part.trim();
            if (token.isEmpty()) {
                continue;
            }
            String key = normalizeKey(token);
            for (String canonical : ALL_SORTED) {
                if (normalizeKey(canonical).equals(key)) {
                    out.add(canonical);
                    break;
                }
            }
        }
        return out;
    }

}
