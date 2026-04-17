package be.mirooz.elitedangerous.dashboard.model.registries.commander;

import be.mirooz.elitedangerous.dashboard.view.common.CommanderStatusComponent;
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
    private boolean isOnFoot = false;

    /** Depuis LoadGame / Fileheader (en-tête EDDN). */
    private String gameVersion;
    private String gameBuild;
    private Boolean horizons;
    private Boolean odyssey;

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

    public void setOnFoot(boolean value) {
        this.isOnFoot = value;
    }

    public boolean isOnFoot() {
        return isOnFoot;
    }

    public void setGameVersion(String gameVersion) {
        this.gameVersion = gameVersion;
    }

    public void setGameBuild(String gameBuild) {
        this.gameBuild = gameBuild;
    }

    public void setHorizons(Boolean horizons) {
        this.horizons = horizons;
    }

    public void setOdyssey(Boolean odyssey) {
        this.odyssey = odyssey;
    }
}
