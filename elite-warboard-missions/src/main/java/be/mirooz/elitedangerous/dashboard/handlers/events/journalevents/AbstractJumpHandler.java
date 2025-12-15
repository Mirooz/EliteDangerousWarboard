package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.ExplorationDataSaleRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.PlaneteRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.SystemVisitedRegistry;
import be.mirooz.elitedangerous.dashboard.service.DirectionReaderService;
import be.mirooz.elitedangerous.dashboard.service.ExplorationService;
import be.mirooz.elitedangerous.dashboard.service.MiningStatsService;
import be.mirooz.elitedangerous.dashboard.service.listeners.ExplorationRefreshNotificationService;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Handler de base pour les évènements de type "jump" (FSD, Carrier, ...).
 * Centralise toute la logique commune pour éviter la duplication de code.
 */
public abstract class AbstractJumpHandler implements JournalEventHandler {

    protected final CommanderStatus commanderStatus = CommanderStatus.getInstance();
    protected final MiningStatsService miningStatsService = MiningStatsService.getInstance();
    protected final PlaneteRegistry planeteRegistry = PlaneteRegistry.getInstance();

    protected final ExplorationRefreshNotificationService notificationService =
            ExplorationRefreshNotificationService.getInstance();
    protected final DirectionReaderService directionReaderService = DirectionReaderService.getInstance();

    /**
     * Libellé utilisé pour les logs (ex: "FSD Jump", "Carrier Jump").
     */
    protected abstract String getJumpLabel();

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            if (jsonNode.has("StarSystem")) {
                String starSystem = jsonNode.get("StarSystem").asText();
                commanderStatus.setCurrentStarSystem(starSystem);
                System.out.println(getJumpLabel() + " detected to - " + starSystem);
                String timestamp = jsonNode.has("timestamp") ? jsonNode.get("timestamp").asText() : null;

                // Terminer la session de minage en cours si elle existe
                if (miningStatsService.isMiningInProgress()) {
                    miningStatsService.endCurrentMiningSession(timestamp);
                    System.out.println("⛏️ Session de minage terminée (" + getJumpLabel() + ")");
                }

                planeteRegistry.clear();
                planeteRegistry.setCurrentStarSystem(starSystem);
                if (SystemVisitedRegistry.getInstance().getSystems().containsKey(starSystem)) {
                    planeteRegistry.setAllPlanetes(SystemVisitedRegistry.getInstance()
                            .getSystems()
                            .get(starSystem)
                            .getCelesteBodies());
                }

                // Ajoute l'ancien dans les visited
                if (planeteRegistry.getCurrentStarSystem() != null) {
                    ExplorationService.getInstance().addOrUpdateSystem(
                            planeteRegistry.getCurrentStarSystem(),
                            planeteRegistry.getAllPlanetes(),
                            timestamp
                    );
                    ExplorationDataSaleRegistry.getInstance().addToOnHold(
                            SystemVisitedRegistry.getInstance().getSystem(planeteRegistry.getCurrentStarSystem())
                    );
                }

                directionReaderService.stopWatchingStatusFile();
                // Notifier le refresh du panneau d'exploration
                ExplorationRefreshNotificationService.getInstance().notifyRefreshRequired();

                notificationService.notifyBodyFilter(null);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de " + getJumpLabel() + ": " + e.getMessage());
        }
    }
}

