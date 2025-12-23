package be.mirooz.elitedangerous.dashboard.view.common;

import be.mirooz.elitedangerous.dashboard.model.commander.CommanderStatus;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class CommanderStatusComponent {

    private static final CommanderStatusComponent INSTANCE = new CommanderStatusComponent();
    public static CommanderStatusComponent getInstance() {
        return INSTANCE;
    }

    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();

    private final StringProperty currentStarSystem = new SimpleStringProperty();
    private final StringProperty currentStationName = new SimpleStringProperty();
    private final StringProperty commanderName = new SimpleStringProperty();
    private final StringProperty FID = new SimpleStringProperty();
    private final BooleanProperty isOnline = new SimpleBooleanProperty();

    private CommanderStatusComponent() {}

    // --- Méthode utilitaire centralisée pour éviter la répétition ---
    private void setOnFxThread(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    // --- Setters thread-safe pour mise à jour des propriétés ---
    public void setCurrentStarSystem(String value) {
        setOnFxThread(() -> currentStarSystem.set(value));
    }

    public void setCurrentStationName(String value) {
        setOnFxThread(() -> currentStationName.set(value));
    }

    public void setCommanderName(String value) {
        setOnFxThread(() -> commanderName.set(value));
    }

    public void setFID(String value) {
        setOnFxThread(() -> FID.set(value));
    }

    public void setOnline(boolean value) {
        setOnFxThread(() -> isOnline.set(value));
    }
}
