// src/main/java/be/mirooz/elitedangerous/edtools/model/PveRow.java
package be.mirooz.elitedangerous.lib.edtools.model;

import lombok.Data;

import java.util.Objects;

/** Représente une ligne de la table PVE d’edtools.cc */
@Data
public class MassacreSystem {
    private final String distanceLy;
    private final String sourceSystem;
    private final String largePads;   // L-pad
    private final String mediumPads;  // M-pad
    private final String smallPads;   // P-pad (petites)
    private final String fed;         // présence fédé
    private final String imp;         // présence empire
    private final String all;         // alliance
    private final String ind;         // indépendant
    private final String targetSystem;
    private final String targetCount;
    private final String resRings;    // ex: "high,low" ou "no rings"


    @Override public String toString() {
        return String.format("%s ly | %s -> %s [%s] | L:%s M:%s P:%s | Fed:%s Imp:%s All:%s Ind:%s | RES:%s",
                distanceLy, sourceSystem, targetSystem,targetCount, largePads, mediumPads, smallPads, fed, imp, all, ind, resRings);
    }

}
