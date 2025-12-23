package be.mirooz.elitedangerous.dashboard.view.exploration;

import be.mirooz.elitedangerous.dashboard.model.exploration.ExplorationMode;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Composant pour sélectionner le mode d'exploration
 */
public class ExplorationModeSelectorComponent implements Initializable {

    @FXML
    private VBox modeSelectorContainer;

    @FXML
    private HBox modeButtonsContainer;

    @FXML
    private Label modeDescriptionLabel;

    private ToggleGroup modeToggleGroup;
    private ExplorationMode currentMode = ExplorationMode.FREE_EXPLORATION;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeModeSelector();
    }

    private void initializeModeSelector() {
        modeToggleGroup = new ToggleGroup();

        // Créer un RadioButton pour chaque mode
        for (ExplorationMode mode : ExplorationMode.values()) {
            RadioButton radioButton = new RadioButton(mode.getDisplayName());
            radioButton.setToggleGroup(modeToggleGroup);
            radioButton.setUserData(mode);
            radioButton.getStyleClass().add("exploration-mode-radio");
            
            // Sélectionner le mode par défaut
            if (mode == ExplorationMode.FREE_EXPLORATION) {
                radioButton.setSelected(true);
            }
            
            // Gérer le changement de sélection
            radioButton.setOnAction(e -> {
                ExplorationMode selectedMode = (ExplorationMode) radioButton.getUserData();
                if (selectedMode != null) {
                    currentMode = selectedMode;
                    updateDescription(selectedMode);
                }
            });
            
            if (modeButtonsContainer != null) {
                modeButtonsContainer.getChildren().add(radioButton);
            }
        }

        // Afficher la description du mode par défaut
        updateDescription(ExplorationMode.FREE_EXPLORATION);
    }

    private void updateDescription(ExplorationMode mode) {
        if (modeDescriptionLabel != null && mode != null) {
            modeDescriptionLabel.setText(mode.getDescription());
        }
    }

    /**
     * Récupère le mode d'exploration actuellement sélectionné
     */
    public ExplorationMode getCurrentMode() {
        return currentMode;
    }
}
