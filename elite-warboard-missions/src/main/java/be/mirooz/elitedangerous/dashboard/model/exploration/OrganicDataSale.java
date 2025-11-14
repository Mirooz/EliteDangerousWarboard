package be.mirooz.elitedangerous.dashboard.model.exploration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Représente une vente complète de données organiques.
 * Contient tous les BioData vendus lors d'une transaction, avec les totaux.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganicDataSale {
    private String timestamp;
    private long marketID;
    
    @Builder.Default
    private List<SoldBioData> bioData = new ArrayList<>();
    
    private long totalValue; // Somme de toutes les valeurs
    private long totalBonus; // Somme de tous les bonus
}

