package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.registries.colonisation.ColonisationConstruction;
import be.mirooz.elitedangerous.dashboard.model.registries.colonisation.ColonisationDocksRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.colonisation.ColonisationRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.colonisation.ConstructionResource;
import be.mirooz.elitedangerous.dashboard.model.registries.colonisation.ConstructionStatus;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public class ColonisationConstructionDepotHandler implements JournalEventHandler {

    private final ColonisationRegistry colonisationRegistry = ColonisationRegistry.getInstance();
    private final ColonisationDocksRegistry colonisationDocksRegistry = ColonisationDocksRegistry.getInstance();

    @Override
    public String getEventType() {
        return "ColonisationConstructionDepot";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            String timestamp = jsonNode.path("timestamp").asText("");
            long marketId = jsonNode.path("MarketID").asLong();
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

            colonisationRegistry.updateConstructionDepot(marketId, progress, status, resources);

            ColonisationConstruction construction = new ColonisationConstruction(
                    timestamp, progress, status, List.copyOf(resources));
            colonisationDocksRegistry.updateConstruction(marketId, construction);

            System.out.println("Colonisation: dépôt de construction MarketID=" + marketId
                    + ", progression=" + progress + ", ressources=" + resources.size() + " lignes");
        } catch (Exception e) {
            System.err.println("Erreur lors du traitement de ColonisationConstructionDepot: " + e.getMessage());
        }
    }
}
