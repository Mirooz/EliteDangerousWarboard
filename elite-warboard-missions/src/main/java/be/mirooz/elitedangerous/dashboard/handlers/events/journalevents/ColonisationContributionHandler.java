package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Journal {@code ColonisationContribution} : apports de stocks au site de construction (dépôt côté chantier).
 * Parsing des champs pour des traitements ultérieurs.
 */
public class ColonisationContributionHandler implements JournalEventHandler {

    @Override
    public String getEventType() {
        return "ColonisationContribution";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        String timestamp = jsonNode.path("timestamp").asText(null);
        String event = jsonNode.path("event").asText(null);
        long marketId = jsonNode.path("MarketID").asLong(0L);
        JsonNode contributions = jsonNode.path("Contributions");

        if (contributions.isArray()) {
            for (JsonNode contribution : contributions) {
                String name = contribution.path("Name").asText(null);
                String nameLocalised = contribution.path("Name_Localised").asText(null);
                int amount = contribution.path("Amount").asInt(0);
            }
        }
    }
}
