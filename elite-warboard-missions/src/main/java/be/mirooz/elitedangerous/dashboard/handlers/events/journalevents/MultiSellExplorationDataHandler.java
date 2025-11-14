package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.exploration.SystemVisited;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.ExplorationDataSaleRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.SystemVisitedRegistry;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler pour l'événement MultiSellExplorationData du journal Elite Dangerous
 * <p>
 * Traite les ventes multiples de données d'exploration.
 * Les données sont accumulées dans une vente en cours jusqu'à l'événement Undocked.
 */
public class MultiSellExplorationDataHandler implements JournalEventHandler {

    private final ExplorationDataSaleRegistry saleRegistry = ExplorationDataSaleRegistry.getInstance();
    @Override
    public String getEventType() {
        return "MultiSellExplorationData";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            String timestamp = jsonNode.path("timestamp").asText();
            long baseValue = jsonNode.path("BaseValue").asLong(0);
            long bonus = jsonNode.path("Bonus").asLong(0);
            long totalEarnings = jsonNode.path("TotalEarnings").asLong(0);

            // Extraire tous les systèmes découverts
            List<SystemVisited> discoveredSystems = new ArrayList<>();
            
            if (jsonNode.has("Discovered") && jsonNode.get("Discovered").isArray()) {
                jsonNode.get("Discovered").forEach(discoveredNode -> {
                    String systemName = discoveredNode.path("SystemName").asText();
                    int numBodies = discoveredNode.path("NumBodies").asInt();
                    if (!SystemVisitedRegistry.getInstance().getSystems().containsKey(systemName)){
                        SystemVisited systemVisited = new SystemVisited();
                        systemVisited.setNumBodies(numBodies);
                        systemVisited.setSold(true);
                        systemVisited.setFirstVisitedTime(timestamp);
                        systemVisited.setLastVisitedTime(timestamp);
                        SystemVisitedRegistry.getInstance().getSystems().put(systemName,systemVisited);
                    }
                    SystemVisited systemVisited = SystemVisitedRegistry.getInstance().getSystem(systemName);
                    discoveredSystems.add(systemVisited);
                    systemVisited.setSold(true);
                });
            }

            // Ajouter à la vente en cours
            saleRegistry.addToCurrentSale(discoveredSystems, baseValue, bonus, totalEarnings, timestamp);

            System.out.printf("📊 Données d'exploration ajoutées: %d systèmes, BaseValue: %d, Bonus: %d, Total: %d%n",
                    discoveredSystems.size(), baseValue, bonus, totalEarnings);
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du traitement de l'événement MultiSellExplorationData: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

