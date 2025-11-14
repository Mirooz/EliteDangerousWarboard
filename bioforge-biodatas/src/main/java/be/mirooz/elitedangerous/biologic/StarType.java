package be.mirooz.elitedangerous.biologic;

import lombok.Getter;

import java.util.EnumSet;
import java.util.List;

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
        if (v.equals("W") || v.equals("WN") || v.equals("WNC") || v.equals("WC") || v.equals("WO"))
            return WOLF_RAYET;

        // fallback (O, B, A, F, G, K, M, TTS…)
        return STAR;
    }


}
