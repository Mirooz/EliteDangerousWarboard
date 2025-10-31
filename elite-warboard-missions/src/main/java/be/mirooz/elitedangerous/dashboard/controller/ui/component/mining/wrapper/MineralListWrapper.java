package be.mirooz.elitedangerous.dashboard.controller.ui.component.mining.wrapper;

import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.Mineral;
import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.MineralType;
import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.MiningMethod;
import be.mirooz.elitedangerous.dashboard.service.MiningService;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper unique pour afficher une liste mixte de minéraux et de séparateurs dans une ComboBox ou ListView.
 */
@Data
public class MineralListWrapper {

    public enum Type {
        SEPARATOR,
        MINERAL
    }

    private final MiningService miningService = MiningService.getInstance();
    private final Type type;
    private final MiningMethod miningMethod; // utilisé si SEPARATOR
    private final Mineral mineral;           // utilisé si MINERAL

    // 🧠 Nouvelles propriétés observables pour l’affichage
    private final StringProperty displayPrice = new SimpleStringProperty("");

    // Constructeur pour un séparateur
    public static MineralListWrapper separator(MiningMethod miningMethod) {
        return new MineralListWrapper(Type.SEPARATOR, miningMethod, null);
    }

    // Constructeur pour un minéral
    public static MineralListWrapper mineral(Mineral mineral) {
        return new MineralListWrapper(Type.MINERAL, mineral.getMiningMethod(), mineral);
    }

    private MineralListWrapper(Type type, MiningMethod miningMethod, Mineral mineral) {
        this.type = type;
        this.miningMethod = miningMethod;
        this.mineral = mineral;
        if (mineral != null) {
            displayPrice.bind(Bindings.createStringBinding(
                    () -> miningService.formatPrice(mineral.getPriceProperty().get()) + " Cr",
                    mineral.getPriceProperty()
            ));
        }
    }

    public boolean isSeparator() {
        return type == Type.SEPARATOR;
    }

    // 🔹 Méthodes utilitaires pour simplifier l’usage


    public void setDisplayPriceError(String message) {
        this.displayPrice.set(message);
    }

    public boolean hasDisplayPrice() {
        return !displayPrice.get().isEmpty();
    }

    public StringProperty displayPriceProperty() {
        return displayPrice;
    }
    public String getDisplayPriceFormatted() {
        return displayPrice.get();
    }

    /**
     * 🔸 Construit une liste organisée avec séparateurs par catégorie et minéraux associés.
     */
    public static ObservableList<MineralListWrapper> createOrganizedMineralsList() {
        List<MineralListWrapper> items = new ArrayList<>();

        // ⚡ Ordre voulu : CORE d'abord, puis LASER
        List<MiningMethod> orderedMethods = List.of(MiningMethod.CORE, MiningMethod.LASER);

        for (MiningMethod method : orderedMethods) {
            items.add(MineralListWrapper.separator(method));

            List<MineralType> minerals = MineralType.all().stream()
                    .filter(m -> m.getMiningMethod() == method)
                    .filter(m -> !m.isTrashValue())
                    .toList();

            for (Mineral mineral : minerals) {
                items.add(MineralListWrapper.mineral(mineral));
            }
        }

        return FXCollections.observableArrayList(items);
    }
}
