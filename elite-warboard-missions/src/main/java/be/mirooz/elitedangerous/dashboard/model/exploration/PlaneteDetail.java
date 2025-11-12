package be.mirooz.elitedangerous.dashboard.model.exploration;

import be.mirooz.elitedangerous.biologic.AtmosphereType;
import be.mirooz.elitedangerous.biologic.BodyType;
import be.mirooz.elitedangerous.biologic.VolcanismType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Modèle représentant les détails d'une planète scannée dans Elite Dangerous.
 * Stocke toutes les informations utiles extraites de l'événement Scan.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaneteDetail {
    
    // Informations de base
    private String bodyName;
    private String starSystem;
    private long systemAddress;
    private int bodyID;
    private BodyType planetClass;
    
    // Propriétés physiques
    private Double temperature; // En Kelvin
    private Double pressureAtm; // Pression en atmosphères (convertie depuis Pascal)
    private Double gravityG; // Gravité en G (convertie depuis m/s²)
    private boolean landable;
    
    // Atmosphère et volcanisme
    private AtmosphereType atmosphere;
    private VolcanismType volcanism;
    
    // Matériaux de surface (nom -> pourcentage)
    private Map<String, Double> materials;
    
    // Statut de découverte
    private boolean wasMapped;
    private boolean wasFootfalled;
    private boolean wasDiscovered;
    
    /**
     * Convertit la pression de Pascal vers atmosphères.
     * 1 atm = 101325 Pascal
     */
    public static double pascalToAtm(double pascal) {
        return pascal / 101325.0;
    }
    
    /**
     * Convertit la gravité de m/s² vers G.
     * 1 G = 9.80665 m/s²
     */
    public static double ms2ToG(double ms2) {
        return ms2 / 9.80665;
    }
}

