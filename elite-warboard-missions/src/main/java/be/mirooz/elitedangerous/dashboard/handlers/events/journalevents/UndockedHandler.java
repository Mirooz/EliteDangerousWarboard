package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.model.registries.ExplorationDataSaleRegistry;
import com.fasterxml.jackson.databind.JsonNode;

public class UndockedHandler implements JournalEventHandler {

    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();
    private final ExplorationDataSaleRegistry explorationRegistry = ExplorationDataSaleRegistry.getInstance();

    @Override
    public String getEventType() {
        return "Undocked";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            String timestamp = jsonNode.path("timestamp").asText();
            commanderStatus.setCurrentStationName("-");
            
            // Finaliser la vente d'exploration en cours
            explorationRegistry.finalizeCurrentSale(timestamp);
            
            System.out.println("Undocked - Vente d'exploration finalisée");
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de Location: " + e.getMessage());
        }
    }
}
