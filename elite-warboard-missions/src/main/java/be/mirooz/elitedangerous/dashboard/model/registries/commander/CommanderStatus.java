package be.mirooz.elitedangerous.dashboard.model.registries.commander;

import be.mirooz.elitedangerous.dashboard.view.common.CommanderStatusComponent;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
     * DTO JSON pour {@code commander-status.json} (commandant seul). Le vaisseau (singleton
     * {@link CommanderShip}) est dans {@code commander-ship.json} via
     * {@code DashboardRegistryJsonPersistence}.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class PersistenceFile {

        private String currentStarSystem;
        private String currentStationName;
        private String currentBodyName;
        private Long currentBodyId;
        private String commanderName;
        private String fid;
        private Boolean isOnline;
        private boolean isOnFoot;
        private Long currentSystemAddress;
        private double[] currentStarPos;
        private String gameVersion;
        private String gameBuild;
        private Boolean horizons;
        private Boolean odyssey;

        public static PersistenceFile fromRuntime(CommanderStatus status) {
            return PersistenceFile.builder()
                    .currentStarSystem(status.getCurrentStarSystem())
                    .currentStationName(status.getCurrentStationName())
                    .currentBodyName(status.getCurrentBodyName())
                    .currentBodyId(status.getCurrentBodyId())
                    .commanderName(status.getCommanderName())
                    .fid(status.getFID())
                    .isOnline(status.getIsOnline())
                    .isOnFoot(status.isOnFoot())
                    .currentSystemAddress(status.getCurrentSystemAddress())
                    .currentStarPos(status.getCurrentStarPos())
                    .gameVersion(status.getGameVersion())
                    .gameBuild(status.getGameBuild())
                    .horizons(status.getHorizons())
                    .odyssey(status.getOdyssey())
                    .build();
        }

        public void restore() {
            CommanderStatus s = CommanderStatus.getInstance();
            synchronized (s) {
                s.currentStarSystem = currentStarSystem;
                s.currentStationName = currentStationName;
                s.currentBodyName = currentBodyName;
                s.currentBodyId = currentBodyId;
                s.commanderName = commanderName;
                s.FID = fid;
                s.isOnline = isOnline;
                s.isOnFoot = isOnFoot;
                s.currentSystemAddress = currentSystemAddress;
                s.currentStarPos = currentStarPos;
                s.gameVersion = gameVersion;
                s.gameBuild = gameBuild;
                s.horizons = horizons;
                s.odyssey = odyssey;
                if (commanderName != null) s.component.setCommanderName(commanderName);
                if (fid != null) s.component.setFID(fid);
                if (currentStarSystem != null) s.component.setCurrentStarSystem(currentStarSystem);
                if (currentStationName != null) s.component.setCurrentStationName(currentStationName);
                if (isOnline != null) s.component.setOnline(isOnline);
            }
        }
    }
}
