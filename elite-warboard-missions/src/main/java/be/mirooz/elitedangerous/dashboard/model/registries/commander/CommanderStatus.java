package be.mirooz.elitedangerous.dashboard.model.registries.commander;

import be.mirooz.elitedangerous.dashboard.view.common.CommanderStatusComponent;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommanderStatus {

    private static final CommanderStatus INSTANCE = new CommanderStatus();

    public static CommanderStatus getInstance() {
        return INSTANCE;
    }

    @JsonIgnore
    private final CommanderStatusComponent component = CommanderStatusComponent.getInstance();

    private String currentStarSystem;
    private String currentStationName;
    private String currentBodyName;
    private Long  currentBodyId;
    private String commanderName;
    private String FID;
    /** Toujours sérialisé (même {@code null}) pour que la reprise JSON fusionne l’état hors-ligne correctement. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
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

    private CommanderStatus() {
    }

    /** Délègue au singleton {@link CommanderShip#getInstance()}. */
    public CommanderShip getShip() {
        return CommanderShip.getInstance();
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

    /**
     * Réaligne la propriété {@code isOnline} de l’UI sur le dernier état en mémoire après un
     * replay journal (handlers hors thread JavaFX peuvent avoir laissé une file {@code runLater}).
     */
    public void syncOnlineToComponent() {
        component.setOnline(Boolean.TRUE.equals(this.isOnline));
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

}
