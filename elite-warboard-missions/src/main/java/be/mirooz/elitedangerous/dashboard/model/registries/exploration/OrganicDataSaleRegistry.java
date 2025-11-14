package be.mirooz.elitedangerous.dashboard.model.registries.exploration;

import be.mirooz.elitedangerous.biologic.BioSpecies;
import be.mirooz.elitedangerous.dashboard.model.exploration.OrganicDataOnHold;
import be.mirooz.elitedangerous.dashboard.model.exploration.OrganicDataSale;
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
    
    // Vente en cours (accumule les données organiques analysées jusqu'à la vente)
    private OrganicDataOnHold currentOrganicDataCredit = null;

    private OrganicDataSaleRegistry() {
    }

    public static OrganicDataSaleRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Ajoute des données organiques analysées à la vente en cours.
     * Appelé lorsqu'un scanType ANALYSE est ajouté.
     * 
     * @param baseValue La valeur de base de l'espèce
     * @param bonusValue La valeur de bonus (première découverte)
     * @param wasFootfalled Indique si la planète a déjà été foulée (si false, c'est une première découverte)
     */
    public void addAnalyzedOrganicData(BioSpecies bioSpecies, boolean wasFootfalled) {
        if (currentOrganicDataCredit == null) {
            // Créer une nouvelle vente en cours
            currentOrganicDataCredit = OrganicDataOnHold.builder()
                    .totalValue(0)
                    .totalBonus(0)
                    .build();
        }
        currentOrganicDataCredit.getBioData().add(bioSpecies);
        
        // Toujours ajouter la baseValue
        currentOrganicDataCredit.setTotalValue(currentOrganicDataCredit.getTotalValue() + bioSpecies.getBaseValue());
        
        // Ajouter le bonusValue seulement si wasFootfalled est false (première découverte)
        if (!wasFootfalled) {
            currentOrganicDataCredit.setTotalBonus(currentOrganicDataCredit.getTotalBonus() + bioSpecies.getBonusValue());
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
    public OrganicDataOnHold getCurrentOrganicDataCredit() {
        return currentOrganicDataCredit;
    }

    /**
     * Réinitialise la vente en cours (après une vente complète).
     */
    public void resetCurrentCredit() {
        currentOrganicDataCredit = null;
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
        currentOrganicDataCredit = null;
    }

    /**
     * Retourne le nombre de ventes dans le registry.
     */
    public int size() {
        return sales.size();
    }
}

