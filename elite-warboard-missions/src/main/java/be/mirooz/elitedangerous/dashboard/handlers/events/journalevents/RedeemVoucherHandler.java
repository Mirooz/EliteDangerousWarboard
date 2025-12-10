package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.registries.combat.DestroyedShipsRegistery;
import be.mirooz.elitedangerous.dashboard.service.listeners.MissionEventNotificationService;
import com.fasterxml.jackson.databind.JsonNode;

public class RedeemVoucherHandler implements JournalEventHandler {
    private final DestroyedShipsRegistery destroyedShipsRegistery;

    public RedeemVoucherHandler() {
        this.destroyedShipsRegistery = DestroyedShipsRegistery.getInstance();
    }

    @Override
    public String getEventType() {
        return "RedeemVoucher";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            System.out.println(jsonNode);
            // Vérifier que c'est un encaissement de bounty
            if (jsonNode.has("Type") && "bounty".equals(jsonNode.get("Type").asText())) {
                // Reset total des statistiques de bounty
                destroyedShipsRegistery.clearBounty();
                System.out.println("Reset complet des statistiques de bounty après encaissement");
            } else if (jsonNode.has("Type") && "CombatBond".equals(jsonNode.get("Type").asText())) {
                // Reset total des statistiques de bounty
                destroyedShipsRegistery.clearCombatBond();
                System.out.println("Reset complet des statistiques de combatbond après encaissement");
            }

            MissionEventNotificationService.getInstance().notifyOnMissionStatusChanged();

        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de RedeemVoucher: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
