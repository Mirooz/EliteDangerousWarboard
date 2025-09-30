package be.mirooz.elitedangerous.dashboard.handlers.events;

import be.mirooz.elitedangerous.dashboard.model.DestroyedShipsList;
import com.fasterxml.jackson.databind.JsonNode;

public class RedeemVoucherHandler implements JournalEventHandler {
    private final DestroyedShipsList destroyedShipsList;

    public RedeemVoucherHandler() {
        this.destroyedShipsList = DestroyedShipsList.getInstance();
    }

    @Override
    public String getEventType() {
        return "RedeemVoucher";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            System.out.println("RedeemVoucher event");
            
            // Vérifier que c'est un encaissement de bounty
            if (jsonNode.has("Type") && "bounty".equals(jsonNode.get("Type").asText())) {
                // Reset total des statistiques de bounty
                destroyedShipsList.clearBounty();
                destroyedShipsList.clearRewards();
                
                System.out.println("Reset complet des statistiques de bounty après encaissement");
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de RedeemVoucher: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
