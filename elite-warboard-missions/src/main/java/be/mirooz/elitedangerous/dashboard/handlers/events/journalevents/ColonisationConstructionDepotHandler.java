package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.colonisation.ColonisationConstruction;
import be.mirooz.elitedangerous.dashboard.model.colonisation.ColonisationJournalContext;
import be.mirooz.elitedangerous.dashboard.model.colonisation.ConstructionResource;
import be.mirooz.elitedangerous.dashboard.model.colonisation.ConstructionStatus;
import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.service.ColonisationService;
import be.mirooz.elitedangerous.dashboard.service.listeners.ColonisationNotificationService;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public class ColonisationConstructionDepotHandler implements JournalEventHandler {

    private final ColonisationService colonisationService = ColonisationService.getInstance();
    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();

    @Override
    public String getEventType() {
        return "ColonisationConstructionDepot";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            String timestamp = jsonNode.path("timestamp").asText("");
            String starSystem = ColonisationJournalContext.resolveStarSystem(jsonNode, commanderStatus);
            long marketId = jsonNode.path("MarketID").asLong();

            if (!colonisationService.isBeaconDeployed(starSystem)) {
                System.out.println("Colonisation: ColonisationConstructionDepot ignoré tant que "
                        + "ColonisationBeaconDeployed n'a pas été reçu (MarketID=" + marketId
                        + (starSystem.isEmpty() ? "" : ", système=« " + starSystem + " »") + ")");
                return;
            }

            double progress = jsonNode.path("ConstructionProgress").asDouble();
            ConstructionStatus status = ConstructionStatus.fromJournalBooleans(
                    jsonNode.path("ConstructionComplete").asBoolean(false),
                    jsonNode.path("ConstructionFailed").asBoolean(false));

            List<ConstructionResource> resources = new ArrayList<>();
            JsonNode arr = jsonNode.path("ResourcesRequired");
            if (arr.isArray()) {
                for (JsonNode row : arr) {
                    resources.add(ConstructionResource.fromResourcesRequiredRow(row));
                }
            }

            ColonisationConstruction construction = new ColonisationConstruction(
                    timestamp, progress, status, List.copyOf(resources));
            colonisationService.applyConstructionDepot(marketId, construction, starSystem, CommanderStatus.getInstance().getCurrentBodyId());
            ColonisationNotificationService.getInstance().notifyColonisationDataChanged();

            System.out.println("Colonisation: dépôt de construction MarketID=" + marketId
                    + (starSystem.isEmpty() ? "" : " (« " + starSystem + " »)")
                    + ", progression=" + progress + ", ressources=" + resources.size() + " lignes");
        } catch (Exception e) {
            System.err.println("Erreur lors du traitement de ColonisationConstructionDepot: " + e.getMessage());
        }
    }
}
