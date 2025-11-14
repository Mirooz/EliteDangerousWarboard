package be.mirooz.elitedangerous.dashboard.model.registries;

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

    private OrganicDataSaleRegistry() {
    }

    public static OrganicDataSaleRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Ajoute une vente de données organiques au registry.
     */
    public void addSale(OrganicDataSale sale) {
        if (sale != null) {
            sales.add(sale);
        }
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
    }

    /**
     * Retourne le nombre de ventes dans le registry.
     */
    public int size() {
        return sales.size();
    }
}

