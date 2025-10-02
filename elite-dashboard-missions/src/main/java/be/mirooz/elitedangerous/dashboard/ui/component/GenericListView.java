package be.mirooz.elitedangerous.dashboard.ui.component;

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
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(componentFactory.apply(item));
                }
            }
        });
        this.setSelectionModel(null);
    }
}
