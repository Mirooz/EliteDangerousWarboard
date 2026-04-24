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
    private String currentBodyName;
    private Long  currentBodyId;
    private String commanderName;
    private String FID;
    private Boolean isOnline;
    private boolean isOnFoot = false;

    /** Dernier {@code SystemAddress} connu (FSDJump / Location / CarrierJump). Requis par certains schémas EDDN. */
    private Long currentSystemAddress;
    /** Dernière position galactique connue (x,y,z) en années-lumière. {@code null} tant que non reçue. */
    private double[] currentStarPos;

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

    public void setCurrentBody(String bodyName, Long bodyId) {
        this.currentBodyName = bodyName;
        this.currentBodyId = bodyId;
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

    public void setCurrentSystemAddress(Long value) {
        this.currentSystemAddress = value;
    }

    public void setCurrentStarPos(double[] value) {
        this.currentStarPos = value;
    }

    /**
     * Restauration depuis un snapshot persisté. Utilisé exclusivement par le
     * {@code PersistenceService} ; le composant UI est mis à jour via les setters.
     */
    public synchronized void applyFullPersistedSnapshot(
            String currentStarSystem,
            String currentStationName,
            String currentBodyName,
            Long currentBodyId,
            String commanderName,
            String FID,
            Boolean isOnline,
            boolean isOnFoot,
            Long currentSystemAddress,
            double[] currentStarPos,
            String gameVersion,
            String gameBuild,
            Boolean horizons,
            Boolean odyssey,
            CommanderShip ship) {
        this.currentStarSystem = currentStarSystem;
        this.currentStationName = currentStationName;
        this.currentBodyName = currentBodyName;
        this.currentBodyId = currentBodyId;
        this.commanderName = commanderName;
        this.FID = FID;
        this.isOnline = isOnline;
        this.isOnFoot = isOnFoot;
        this.currentSystemAddress = currentSystemAddress;
        this.currentStarPos = currentStarPos;
        this.gameVersion = gameVersion;
        this.gameBuild = gameBuild;
        this.horizons = horizons;
        this.odyssey = odyssey;
        this.ship = ship;

        if (commanderName != null) component.setCommanderName(commanderName);
        if (FID != null) component.setFID(FID);
        if (currentStarSystem != null) component.setCurrentStarSystem(currentStarSystem);
        if (currentStationName != null) component.setCurrentStationName(currentStationName);
        if (isOnline != null) component.setOnline(isOnline);
    }
}
