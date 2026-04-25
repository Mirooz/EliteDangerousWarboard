package be.mirooz.elitedangerous.dashboard.view.common;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public class DialogComponent {

    private final String fxmlPath;
    private final Pane preloadedRoot;
    private final String cssPath;
    private final String title;
    private final double width;
    private final double height;

    private Stage stage;

    /** Dialogue chargé depuis un FXML (ex. configuration). */
    public DialogComponent(String fxmlPath, String cssPath, String title, double width, double height) {
        this.fxmlPath = fxmlPath;
        this.preloadedRoot = null;
        this.cssPath = cssPath;
        this.title = title;
        this.width = width;
        this.height = height;
    }

    /** Même rendu que la config : contenu déjà construit (racine type {@code StackPane} + styles {@code config-dialog}). */
    public DialogComponent(Pane rootContent, String cssPath, String title, double width, double height) {
        this.fxmlPath = null;
        this.preloadedRoot = rootContent;
        this.cssPath = cssPath;
        this.title = title;
        this.width = width;
        this.height = height;
    }

    public void init(Window owner) {
        try {
            Pane content;
            if (fxmlPath != null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                content = loader.load();
            } else {
                if (preloadedRoot == null) {
                    throw new IllegalStateException("No dialog content");
                }
                content = preloadedRoot;
            }

            Scene scene = new Scene(content, width, height);
            scene.setFill(Color.TRANSPARENT);
            if (cssPath != null) {
                scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
            }

            this.stage = new Stage();
            this.stage.setTitle(title);
            this.stage.setScene(scene);

            this.stage.initModality(Modality.WINDOW_MODAL);
            if (owner != null) {
                this.stage.initOwner(owner);
            }
            this.stage.setAlwaysOnTop(false);
            this.stage.requestFocus();
            this.stage.setResizable(false);
            /* Pas de barre de titre Windows : cadre dessiné par le thème (.config-content). */
            this.stage.initStyle(StageStyle.UNDECORATED);

        } catch (Exception e) {
            String id = fxmlPath != null ? fxmlPath : "(contenu direct)";
            throw new RuntimeException("Erreur lors de l'initialisation du dialogue : " + id, e);
        }
    }

    public Stage getStage() {
        return stage;
    }

    public void showAndWait() {
        if (stage == null) {
            throw new IllegalStateException("Dialog not initialized");
        }
        stage.centerOnScreen();
        stage.showAndWait();
    }

}
