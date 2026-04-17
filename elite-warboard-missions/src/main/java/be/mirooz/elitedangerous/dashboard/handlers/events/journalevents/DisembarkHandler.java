package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.biologic.BioSpecies;
import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.model.exploration.ACelesteBody;
import be.mirooz.elitedangerous.dashboard.model.exploration.PlaneteDetail;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.PlaneteRegistry;
import be.mirooz.elitedangerous.dashboard.service.listeners.ExplorationRefreshNotificationService;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;

/**
 * Handler pour l'événement Disembark du journal Elite Dangerous
 * <p>
 * Exemple d'événement :
 * {
 * "timestamp":"2025-12-04T17:46:26Z",
 * "event":"Disembark",
 * "SRV":false,
 * "Taxi":false,
 * "Multicrew":false,
 * "ID":38,
 * "StarSystem":"Wregoe AB-F d11-72",
 * "SystemAddress":2484479215971,
 * "Body":"Wregoe AB-F d11-72 B 6 c",
 * "BodyID":29,
 * "OnStation":false,
 * "OnPlanet":true
 * }
 */
public class DisembarkHandler implements JournalEventHandler {

    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();
    private final ExplorationRefreshNotificationService notificationService = ExplorationRefreshNotificationService.getInstance();

    @Override
    public void handle(JsonNode event) {
        try {
            commanderStatus.setOnFoot(true);
            System.out.println("🚶 Commande à pied (Disembark, SRV=false)");
            // Notifier le changement d'état
            notificationService.notifyOnFootStateChanged(true);
            String timestamp = event.get("timestamp").asText();
            String body = event.get("Body").asText();
            Integer bodyID = event.get("BodyID").asInt();
            Optional<ACelesteBody> bodyOpt = PlaneteRegistry.getInstance().getByBodyID(bodyID);
            if (bodyOpt.isPresent() && bodyOpt.get() instanceof PlaneteDetail planet) {
                // Vérifier s'il y a au moins une espèce non collectée
                boolean hasUncollectedExobio = false;
                if (planet.getNumSpeciesDetected() != null && planet.getNumSpeciesDetected() > planet.getConfirmedSpecies().stream().filter(BioSpecies::isCollected).count()) {
                    hasUncollectedExobio = true;
                }


                // Si la planète a des exobio non collectés, filtrer la liste pour n'afficher que cette planète
                if (hasUncollectedExobio) {
                    notificationService.notifyBodyFilter(bodyID);
                } else {
                    // Sinon, désactiver le filtre
                    notificationService.notifyBodyFilter(null);
                }
            } else {
                // Si ce n'est pas une planète ou si elle n'est pas trouvée, désactiver le filtre
                notificationService.notifyBodyFilter(null);
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du traitement de l'événement Disembark: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public String getEventType() {
        return "Disembark";
    }
}

