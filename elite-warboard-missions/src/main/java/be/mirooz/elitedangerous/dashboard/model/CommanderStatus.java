package be.mirooz.elitedangerous.dashboard.model;

import be.mirooz.elitedangerous.dashboard.controller.ui.component.CommanderStatusComponent;
import lombok.Getter;
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

    private CommanderShip ship;

    public void setShip(CommanderShip ship) {
        this.ship = ship;
    }


    private CommanderStatus() {
    }

    public void setCurrentStarSystem(String value) {
        this.currentStarSystem = value;
        component.setCurrentStarSystem(value);
    }

    public void setCurrentStationName(String value) {
        this.currentStationName = value;
        component.setCurrentStationName(value);
    }

    public void setCommanderName(String value) {
        this.commanderName = value;
        component.setCommanderName(value);
    }

    public void setFID(String value) {
        this.FID = value;
        component.setFID(value);
    }

    public void setOnline(boolean value) {
        this.isOnline = value;
        component.setOnline(value);
    }
}
