package be.mirooz.elitedangerous.dashboard.controller.ui.component;

import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

import java.util.function.Function;

public class GenericListView<T> extends ListView<T> {

    private Function<T, Node> componentFactory;

    public GenericListView() {
        super();
    }

    public void setComponentFactory(Function<T, Node> factory) {
        this.componentFactory = factory;

        this.setCellFactory(lv -> new ListCell<>() {

            private final ChangeListener<Object> propertyListener = (obs, oldVal, newVal) -> updateCell(getItem());

            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    updateCell(item);
                }
            }

            private void updateCell(T item) {
                if (item != null) {
                    setGraphic(componentFactory.apply(item));
                } else {
                    setGraphic(null);
                }
            }
        });

        // Supprime toute sélection possible (utile pour une liste purement visuelle)
        this.setSelectionModel(null);
    }
}
