package be.mirooz.elitedangerous.lib.edtools.model;

import lombok.Data;
import java.util.Map;
import java.util.HashMap;

/**
 * Modèle représentant les détails d'un hotspot de minage depuis edtools.cc
 */
@Data
public class MiningHotspot {
    private String systemName;
    private String ringName;
    private String ringType;
    private int hotspotCount;
    private int lightSeconds;
    private double distanceFromReference;
    private Map<String, Integer> mineralHotspots; // Nom du minéral -> nombre de hotspots
    
    public MiningHotspot() {
        this.mineralHotspots = new HashMap<>();
    }
    
    /**
     * Ajoute un minéral avec son nombre de hotspots
     */
    public void addMineral(String mineralName, int count) {
        this.mineralHotspots.put(mineralName, count);
    }

}
