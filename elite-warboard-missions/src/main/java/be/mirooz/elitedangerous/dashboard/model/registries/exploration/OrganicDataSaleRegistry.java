package be.mirooz.elitedangerous.dashboard.model.registries.exploration;

import be.mirooz.elitedangerous.biologic.BioSpecies;
import be.mirooz.elitedangerous.dashboard.model.exploration.OrganicDataOnHold;
import be.mirooz.elitedangerous.dashboard.model.exploration.OrganicDataSale;
import be.mirooz.elitedangerous.dashboard.service.ExplorationService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Data;

import java.util.List;

/**
 * Registry pour stocker les ventes de données organiques.
 * Singleton observable pour la UI.
 */
@Data
public class OrganicDataSaleRegistry {

    private static final OrganicDataSaleRegistry INSTANCE = new OrganicDataSaleRegistry();

    private final ObservableList<OrganicDataSale> sales = FXCollections.observableArrayList();
    private final ExplorationService explorationService = ExplorationService.getInstance();
    
    // Vente en cours (accumule les données organiques analysées jusqu'à la vente)
    private OrganicDataOnHold currentOrganicDataOnHold = null;

    private OrganicDataSaleRegistry() {
    }

    public static OrganicDataSaleRegistry getInstance() {
        return INSTANCE;
    }

    public void addAnalyzedOrganicData(BioSpecies bioSpecies, boolean wasFootfalled) {
        explorationService.clearCurrentBiologicalAnalysis();
        if (currentOrganicDataOnHold == null) {
            // Créer une nouvelle vente en cours
            currentOrganicDataOnHold = OrganicDataOnHold.builder()
                    .totalValue(0)
                    .totalBonus(0)
                    .build();
        }
        currentOrganicDataOnHold.getBioData().add(bioSpecies);
        
        // Toujours ajouter la baseValue
        currentOrganicDataOnHold.setTotalValue(currentOrganicDataOnHold.getTotalValue() + bioSpecies.getBaseValue());
        
        // Ajouter le bonusValue seulement si wasFootfalled est false (première découverte)
        if (!wasFootfalled) {
            currentOrganicDataOnHold.setTotalBonus(currentOrganicDataOnHold.getTotalBonus() + bioSpecies.getBonusValue());
        }
    }

    /**
     * Ajoute une vente de données organiques au registry.
     * Utilisé par SellOrganicDataHandler pour enregistrer une vente complète.
     */
    public void addSale(OrganicDataSale sale) {
        if (sale != null) {
            sales.add(sale);
        }
    }

    /**
     * Récupère la vente en cours (crédit de données organiques actuelles).
     */
    public OrganicDataOnHold getCurrentOrganicDataOnHold() {
        return currentOrganicDataOnHold;
    }

    /**
     * Réinitialise la vente en cours (après une vente complète).
     */
    public void resetCurrentCredit() {
        currentOrganicDataOnHold = null;
    }

    /**
     * Récupère toutes les ventes.
     */
    public List<OrganicDataSale> getAllSales() {
        return sales;
    }

    /**
     * Vide le registry.
     */
    public void clear() {
        sales.clear();
        currentOrganicDataOnHold = null;
    }

    /**
     * Retourne le nombre de ventes dans le registry.
     */
    public int size() {
        return sales.size();
    }
}

