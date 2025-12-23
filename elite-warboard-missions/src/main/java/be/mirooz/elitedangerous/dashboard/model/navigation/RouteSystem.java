package be.mirooz.elitedangerous.dashboard.model.navigation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Représente un système dans une route de navigation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteSystem {
    /**
     * Nom du système
     */
    private String systemName;
    
    /**
     * Adresse du système
     */
    private long systemAddress;
    
    /**
     * Classe de l'étoile
     */
    private String starClass;
    
    /**
     * Position du système (coordonnées en années-lumière)
     */
    private double[] starPos;
    
    /**
     * Distance en années-lumière par rapport au système précédent
     * 0.0 pour le premier système (système actuel)
     */
    private double distanceFromPrevious;
}

