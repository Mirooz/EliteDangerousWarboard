package be.mirooz.elitedangerous.dashboard.controller.ui.component;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class DialogComponent {

    private final String fxmlPath;
    private final String cssPath;
    private final String title;
    private final double width;
    private final double height;

    private Stage stage;
    public DialogComponent(String fxmlPath, String cssPath, String title, double width, double height) {
        this.fxmlPath = fxmlPath;
        this.cssPath = cssPath;
        this.title = title;
        this.width = width;
        this.height = height;
    }

    public void init(Stage owner) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Pane content = loader.load();

            Scene scene = new Scene(content, width, height);
            if (cssPath != null) {
                scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
            }

            this.stage = new Stage();
            this.stage.setTitle(title);
            this.stage.setScene(scene);
            this.stage.initModality(Modality.APPLICATION_MODAL);
            this.stage.initOwner(owner);
            this.stage.setResizable(false);
            this.stage.initStyle(StageStyle.UNDECORATED);

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'initialisation du dialogue : " + fxmlPath, e);
        }
    }

    public void showAndWait() {
        if (stage == null) throw new IllegalStateException("Dialog not initialized");
        stage.centerOnScreen();
        stage.showAndWait();
    }

}
