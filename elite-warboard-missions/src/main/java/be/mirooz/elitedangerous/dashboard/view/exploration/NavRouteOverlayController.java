package be.mirooz.elitedangerous.dashboard.view.exploration;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur pour l'overlay de la route de navigation (uniquement les boules)
 */
public class NavRouteOverlayController implements Initializable {

    @FXML
    private Pane routeSystemsPane;
    
    @FXML
    private Label remainingJumpsLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // L'initialisation sera faite par NavRouteOverlayComponent
    }

    public Pane getRouteSystemsPane() {
        return routeSystemsPane;
    }
    
    public Label getRemainingJumpsLabel() {
        return remainingJumpsLabel;
    }
}

