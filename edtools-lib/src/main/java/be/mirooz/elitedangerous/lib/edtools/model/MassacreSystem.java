// src/main/java/be/mirooz/elitedangerous/edtools/model/PveRow.java
package be.mirooz.elitedangerous.lib.edtools.model;

import java.util.Objects;

/** Représente une ligne de la table PVE d’edtools.cc */
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
    private final String resRings;    // ex: "high,low" ou "no rings"

    public MassacreSystem(String distanceLy, String sourceSystem, String largePads, String mediumPads, String smallPads,
                          String fed, String imp, String all, String ind, String targetSystem, String resRings) {
        this.distanceLy = distanceLy;
        this.sourceSystem = sourceSystem;
        this.largePads = largePads;
        this.mediumPads = mediumPads;
        this.smallPads = smallPads;
        this.fed = fed;
        this.imp = imp;
        this.all = all;
        this.ind = ind;
        this.targetSystem = targetSystem;
        this.resRings = resRings;
    }

    public String getDistanceLy() { return distanceLy; }
    public String getSourceSystem() { return sourceSystem; }
    public String getLargePads() { return largePads; }
    public String getMediumPads() { return mediumPads; }
    public String getSmallPads() { return smallPads; }
    public String getFed() { return fed; }
    public String getImp() { return imp; }
    public String getAll() { return all; }
    public String getInd() { return ind; }
    public String getTargetSystem() { return targetSystem; }
    public String getResRings() { return resRings; }

    @Override public String toString() {
        return String.format("%s ly | %s -> %s | L:%s M:%s P:%s | Fed:%s Imp:%s All:%s Ind:%s | RES:%s",
                distanceLy, sourceSystem, targetSystem, largePads, mediumPads, smallPads, fed, imp, all, ind, resRings);
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MassacreSystem)) return false;
        MassacreSystem that = (MassacreSystem) o;
        return Objects.equals(distanceLy, that.distanceLy)
                && Objects.equals(sourceSystem, that.sourceSystem)
                && Objects.equals(largePads, that.largePads)
                && Objects.equals(mediumPads, that.mediumPads)
                && Objects.equals(smallPads, that.smallPads)
                && Objects.equals(fed, that.fed)
                && Objects.equals(imp, that.imp)
                && Objects.equals(all, that.all)
                && Objects.equals(ind, that.ind)
                && Objects.equals(targetSystem, that.targetSystem)
                && Objects.equals(resRings, that.resRings);
    }

    @Override public int hashCode() {
        return Objects.hash(distanceLy, sourceSystem, largePads, mediumPads, smallPads, fed, imp, all, ind, targetSystem, resRings);
    }
}
