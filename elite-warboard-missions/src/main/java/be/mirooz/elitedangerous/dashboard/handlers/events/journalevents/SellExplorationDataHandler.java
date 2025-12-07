package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.exploration.SystemVisited;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.ExplorationDataSaleRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.SystemVisitedRegistry;
import be.mirooz.elitedangerous.dashboard.service.listeners.ExplorationRefreshNotificationService;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler pour l'événement MultiSellExplorationData du journal Elite Dangerous
 * <p>
 * Traite les ventes multiples de données d'exploration.
 * Les données sont accumulées dans une vente en cours jusqu'à l'événement Undocked.
 */
public class SellExplorationDataHandler implements JournalEventHandler {

    private final ExplorationDataSaleRegistry saleRegistry = ExplorationDataSaleRegistry.getInstance();
    @Override
    public String getEventType() {
        return "SellExplorationData";
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
            
            if (jsonNode.has("Systems") && jsonNode.get("Systems").isArray()) {
                jsonNode.get("Systems").forEach(discoveredNode -> {
                    String systemName = discoveredNode.asText();
                    if (!SystemVisitedRegistry.getInstance().getSystems().containsKey(systemName)){
                        SystemVisited systemVisited = new SystemVisited();
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
            // Supprimer tout les credit d'exploration on Hold
            ExplorationDataSaleRegistry.getInstance().clearOnHold();
            System.out.printf("📊 Données d'exploration ajoutées: %d systèmes, BaseValue: %d, Bonus: %d, Total: %d%n",
                    discoveredSystems.size(), baseValue, bonus, totalEarnings);
            // Notifier le refresh du panneau d'exploration
            ExplorationRefreshNotificationService.getInstance().notifyRefreshRequired();
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du traitement de l'événement SellExplorationData: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

