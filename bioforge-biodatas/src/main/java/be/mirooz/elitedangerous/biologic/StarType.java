package be.mirooz.elitedangerous.biologic;

import lombok.Getter;

@Getter
public enum StarType {

    STAR("star", 1200),
    BLACK_HOLE("black hole", 22628),
    NEUTRON_STAR("neutron star", 22628),
    WHITE_DWARF("white dwarf", 14057),
    HERBIG("herbig ae/be", 5225),

    // Brown dwarfs (L, T, Y)
    BROWN_DWARF("brown dwarf", 1200),

    // Wolf-Rayet (W, WC, WN, WNC, WO)
    WOLF_RAYET("wolf-rayet", 1200);
    private final String label;
    private final int kValue;

    StarType(String label, int kValue) {
        this.label = label;
        this.kValue = kValue;
    }

    public static StarType fromString(String value) {
        if (value == null || value.isBlank()) return STAR;

        String v = value.toUpperCase();

        // Black Hole
        if (v.equals("H")) return BLACK_HOLE;

        // Neutron star
        if (v.equals("N")) return NEUTRON_STAR;

        // White dwarfs
        if (v.startsWith("D")) return WHITE_DWARF;

        // Herbig Ae/Be
        if (v.equals("AEBE")) return HERBIG;

        // Brown dwarfs
        if (v.equals("L") || v.equals("T") || v.equals("Y"))
            return BROWN_DWARF;

        // Wolf-Rayet
        if (v.startsWith("W"))
            return WOLF_RAYET;

        // fallback (O, B, A, F, G, K, M, TTS…)
        return STAR;
    }

    /**
     * Retourne le nom du fichier image correspondant au type d'étoile
     * Les images sont basées sur celles disponibles sur EDSM
     * @return le nom du fichier image (ex: "Star_G.png")
     */
    public String getImageName() {
        return switch (this) {
            case BLACK_HOLE -> "Star_Black_Hole.png";
            case NEUTRON_STAR -> "Star_Neutron.png";
            case WHITE_DWARF -> "Star_D.png";
            case HERBIG -> "Star_Herbig_Ae_Be.png";
            case BROWN_DWARF -> "Star_L.png"; // Utilise L comme image par défaut pour les naines brunes
            case WOLF_RAYET -> "Star_Wolf_Rayet.png";
            case STAR -> "Star_G.png"; // Étoile normale (type G comme le soleil)
        };
    }

}
