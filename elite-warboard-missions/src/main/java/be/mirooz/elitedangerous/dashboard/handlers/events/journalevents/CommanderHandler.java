package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.service.DashboardService;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.service.MiningStatsService;
import be.mirooz.elitedangerous.dashboard.controller.ui.manager.PopupManager;
import javafx.application.Platform;
import com.fasterxml.jackson.databind.JsonNode;

public class CommanderHandler implements JournalEventHandler {

    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();
    private final LocalizationService localizationService = LocalizationService.getInstance();
    private final PopupManager popupManager = PopupManager.getInstance();
    private final MiningStatsService miningStatsService = MiningStatsService.getInstance();

    @Override
    public String getEventType() {
        return "Commander";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            if (jsonNode.has("Name")) {
                String name = jsonNode.get("Name").asText();
                String fid = jsonNode.get("FID").asText();

                // Vérifier si c'est un nouveau commandant
                String currentCommanderName = commanderStatus.getCommanderName();
                String currentFID = commanderStatus.getFID();

                boolean isNewCommander = currentCommanderName == null ||
                        currentFID == null ||
                        !currentFID.equals(fid);

                // Mettre à jour le statut du commandant
                commanderStatus.setCommanderName(name);
                commanderStatus.setFID(fid);

                System.out.println("Commandant - " + name + " - " + fid);
                
                // Reprendre la session de minage suspendue si elle existe
                if (miningStatsService.isMiningSessionSuspended()) {
                    String timestamp = jsonNode.has("timestamp") ? jsonNode.get("timestamp").asText() : null;
                    miningStatsService.resumeMiningSession(timestamp);
                    System.out.println("▶️ Session de minage reprise (Commander)");
                }

                // Si c'est un nouveau commandant, afficher le popup et relire les journaux
                if (isNewCommander && currentCommanderName != null) {
                    showNewCommanderPopup(name);
                    rereadAllJournalsForNewCommander();
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de Commander: " + e.getMessage());
        }
    }

    /**
     * Affiche un popup indiquant qu'un nouveau commandant a été détecté
     */
    private void showNewCommanderPopup(String commanderName) {
        Platform.runLater(() -> {
            try {
                String message = localizationService.getString("commander.new_detected") + commanderName;
                // Afficher le popup au centre de l'écran
                javafx.stage.Window primaryWindow = javafx.stage.Stage.getWindows().stream()
                        .filter(window -> window.isShowing() && !window.getScene().getRoot().getChildrenUnmodifiable().isEmpty())
                        .findFirst()
                        .orElse(null);

                if (primaryWindow != null) {
                    popupManager.showWarningPopup(message, 0, 0, primaryWindow);
                }
            } catch (Exception e) {
                System.err.println("Erreur lors de l'affichage du popup de nouveau commandant: " + e.getMessage());
            }
        });
    }

    /**
     * Relit tous les fichiers journal pour le nouveau commandant
     */
    private void rereadAllJournalsForNewCommander() {
        Platform.runLater(() -> {
            try {
                // Utiliser initActiveMissions pour bien tout reset et initialiser les batch listeners
                DashboardService dashboardService = DashboardService.getInstance();
                dashboardService.initActiveMissions();

            } catch (Exception e) {
                System.err.println("Erreur lors de la relecture des journaux pour le nouveau commandant: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
