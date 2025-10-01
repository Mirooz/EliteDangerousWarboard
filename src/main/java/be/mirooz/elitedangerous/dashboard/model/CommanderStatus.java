package be.mirooz.elitedangerous.dashboard.model;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Getter
@ToString
public class CommanderStatus {
    private static final CommanderStatus INSTANCE = new CommanderStatus();
    private CommanderStatus() {}
    public static CommanderStatus getInstance() {
        return INSTANCE;
    }

    private final StringProperty currentStarSystem= new SimpleStringProperty();
    private final StringProperty currentStationName= new SimpleStringProperty();
    private final StringProperty commanderName= new SimpleStringProperty();
    private final StringProperty FID= new SimpleStringProperty();
    private final BooleanProperty isOnline = new SimpleBooleanProperty();

    public String getCommanderNameString(){
        return this.commanderName.get();
    }
    public String getFIDString(){
        return this.FID.get();
    }
    public String getCurrentStarSystemString(){
        return this.currentStarSystem.get();
    }
    public String getCurrentStationNameString(){
        return this.currentStationName.get();
    }
    public void setCurrentStarSystem(String currentStarSystem) {
        if (Platform.isFxApplicationThread()) {
            this.currentStarSystem.set(currentStarSystem);
        } else {
            Platform.runLater(() -> this.currentStarSystem.set(currentStarSystem));
        }
    }

    public void setCommanderName(String commanderName) {
        if (Platform.isFxApplicationThread()) {
            this.commanderName.set(commanderName);
        } else {
            Platform.runLater(() -> this.commanderName.set(commanderName));
        }
    }

    public void setCurrentStationName(String currentStationName) {
        if (Platform.isFxApplicationThread()) {
            this.currentStationName.set(currentStationName);
        } else {
            Platform.runLater(() -> this.currentStationName.set(currentStationName));
        }
    }
    public void setFID(String FID) {
        if (Platform.isFxApplicationThread()) {
            this.FID.set(FID);
        } else {
            Platform.runLater(() -> this.FID.set(FID));
        }
    }
    public void setOnline(boolean isOnline) {
        if (Platform.isFxApplicationThread()) {
            this.isOnline.set(isOnline);
        } else {
            Platform.runLater(() -> this.isOnline.set(isOnline));
        }
    }
}
