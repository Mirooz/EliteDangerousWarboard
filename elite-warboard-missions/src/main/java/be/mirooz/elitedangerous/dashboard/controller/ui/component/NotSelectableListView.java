package be.mirooz.elitedangerous.dashboard.controller.ui.component;

import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

import java.util.function.Function;

public class NotSelectableListView<T> extends ListView<T> {

    private Function<T, Node> componentFactory;

    public NotSelectableListView() {
        super();
    }

    public void setComponentFactory(Function<T, Node> factory) {
        this.componentFactory = factory;

        this.setCellFactory(lv -> new ListCell<>() {
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
        this.setSelectionModel(new NoSelectionModel<>());
    }
}
