package be.mirooz.elitedangerous.dashboard.view.common;

import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.util.function.Function;

public class NotSelectableListView<T> extends ListView<T> {

    private Function<T, Node> componentFactory;

    public NotSelectableListView() {
        super();

        // Masquer la barre horizontale du skin quand le skin est prêt
        this.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin == null) return;
            Platform.runLater(() -> this.lookupAll(".scroll-bar").forEach(n -> {
                if (n instanceof ScrollBar sb && sb.getOrientation() == Orientation.HORIZONTAL) {
                    sb.setVisible(false);
                    sb.setManaged(false);
                }
            }));
        });
    }

    public void setComponentFactory(Function<T, Node> factory) {
        this.componentFactory = factory;

        this.setCellFactory(lv -> new ListCell<>() {
            // Wrapper qui s'ajuste à la largeur de la ListView
            private final HBox wrapper = new HBox();

            {
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                // Important : la cellule suit la largeur de la ListView (moins un petit padding)
                wrapper.prefWidthProperty().bind(lv.widthProperty().subtract(16));
            }

            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }

                Node content = componentFactory.apply(item);

                // Permet au contenu de se compresser/étendre selon la place
                HBox.setHgrow(content, Priority.ALWAYS);
                content.maxWidth(Double.MAX_VALUE);

                wrapper.getChildren().setAll(content);
                setGraphic(wrapper);
            }
        });

        // Liste purement visuelle
        this.setSelectionModel(new NoSelectionModel<>());
    }
}
