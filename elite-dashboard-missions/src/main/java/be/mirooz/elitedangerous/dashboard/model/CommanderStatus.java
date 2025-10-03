package be.mirooz.elitedangerous.dashboard.model;

import be.mirooz.elitedangerous.dashboard.ui.component.CommanderStatusComponent;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@ToString
public class CommanderStatus {

    private static final CommanderStatus INSTANCE = new CommanderStatus();

    public static CommanderStatus getInstance() {
        return INSTANCE;
    }

    private final CommanderStatusComponent component = CommanderStatusComponent.getInstance();

    private String currentStarSystem;
    private String currentStationName;
    private String commanderName;
    private String FID;
    private Boolean isOnline;
    @Setter
    private boolean uiUpdatesEnabled = true;

    private CommanderStatus() {
    }

    public void flushToUI() {
        component.setCurrentStarSystem(currentStarSystem);
        component.setCurrentStationName(currentStationName);
        component.setCommanderName(commanderName);
        component.setFID(FID);
        component.setOnline(isOnline);
    }
    public void setCurrentStarSystem(String value) {
        this.currentStarSystem = value;
        if (uiUpdatesEnabled)
            component.setCurrentStarSystem(value);
    }

    public void setCurrentStationName(String value) {
        this.currentStationName = value;
        if (uiUpdatesEnabled)
            component.setCurrentStationName(value);
    }

    public void setCommanderName(String value) {
        this.commanderName = value;
        if (uiUpdatesEnabled)
            component.setCommanderName(value);
    }

    public void setFID(String value) {
        this.FID = value;
        if (uiUpdatesEnabled)
            component.setFID(value);
    }

    public void setOnline(boolean value) {
        this.isOnline = value;
        if (uiUpdatesEnabled)
            component.setOnline(value);
    }
}
