package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.registries.colonisation.ColonisationRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.colonisation.ConstructionResource;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public class ColonisationConstructionDepotHandler implements JournalEventHandler {

    private final ColonisationRegistry colonisationRegistry = ColonisationRegistry.getInstance();

    @Override
    public String getEventType() {
        return "ColonisationConstructionDepot";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            long marketId = jsonNode.path("MarketID").asLong();
            double progress = jsonNode.path("ConstructionProgress").asDouble();
            boolean complete = jsonNode.path("ConstructionComplete").asBoolean(false);
            boolean failed = jsonNode.path("ConstructionFailed").asBoolean(false);

            List<ConstructionResource> resources = new ArrayList<>();
            JsonNode arr = jsonNode.path("ResourcesRequired");
            if (arr.isArray()) {
                for (JsonNode row : arr) {
                    String name = row.path("Name_Localised").asText(row.path("Name").asText(""));
                    int required = row.path("RequiredAmount").asInt();
                    int provided = row.path("ProvidedAmount").asInt();
                    long payment = row.path("Payment").asLong();
                    resources.add(new ConstructionResource(name, required, provided, payment));
                }
            }

            colonisationRegistry.updateConstructionDepot(marketId, progress, complete, failed, resources);
            System.out.println("Colonisation: dépôt de construction MarketID=" + marketId
                    + ", progression=" + progress + ", ressources=" + resources.size() + " lignes");
        } catch (Exception e) {
            System.err.println("Erreur lors du traitement de ColonisationConstructionDepot: " + e.getMessage());
        }
    }
}
