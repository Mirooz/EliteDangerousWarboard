package be.mirooz.elitedangerous.dashboard.controller.ui.wrapper;
import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.Mineral;
import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.MineralType;
import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.MiningMethod;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Data;

import java.util.ArrayList;
import java.util.Comparator;
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

    private final Type type;
    private final MiningMethod miningMethod;   // utilisé si SEPARATOR
    private final Mineral mineral;   // utilisé si MINERAL

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
    }


    public boolean isSeparator() {
        return type == Type.SEPARATOR;
    }
    /**
     * 🔸 Construit une liste organisée avec séparateurs par catégorie et minéraux associés.
     */
    public static ObservableList<MineralListWrapper> createOrganizedMineralsList() {
        List<MineralListWrapper> items = new ArrayList<>();

        // ⚡ Ordre voulu : CORE d'abord, puis LASER
        List<MiningMethod> orderedMethods = List.of(MiningMethod.CORE, MiningMethod.LASER);

        for (MiningMethod method : orderedMethods) {
            // Ajout du séparateur
            items.add(MineralListWrapper.separator(method));

            // Filtrer et trier les minéraux pour cette méthode
            List<MineralType> minerals = MineralType.all().stream()
                    .filter(m -> m.getMiningMethod() == method)
                    .toList();

            for (Mineral mineral : minerals) {
                items.add(MineralListWrapper.mineral(mineral));
            }
        }

        return FXCollections.observableArrayList(items);
    }
}
