package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.commander.Mission;
import be.mirooz.elitedangerous.dashboard.model.registries.combat.MissionsRegistry;
import be.mirooz.elitedangerous.dashboard.service.listeners.MissionEventNotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class MissionRedirectedHandler implements JournalEventHandler {
    
    private final MissionsRegistry missionList = MissionsRegistry.getInstance();

    @Override
    public String getEventType() {
        return "MissionRedirected";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            String missionId = jsonNode.get("MissionID").asText();
            Mission mission = missionList.getGlobalMissionMap().get(missionId);
            if (mission == null){
                System.out.println("[Error redirected] " + missionId + " not found");
                return;
            }
            // Vérifier si c'est une mission de massacre et qu'elle st bien complétée
            if (notConsideredAsCompleted(mission)) {
                System.out.println("[Error count] Mission de massacre " + mission.getId() + " kill = " + mission.getCurrentCount() + "/" + mission.getTargetCount());
                mission.setCurrentCount(mission.getTargetCount());
               // mission.setStatus(MissionStatus.COMPLETED);

               // simuleBounty(jsonNode.get("timestamp").asText(), mission);
            }
            else if (mission.isShipMassacre()){
                System.out.println("Mission " + missionId + " Redirected : Completed");
            }

            MissionEventNotificationService.getInstance().notifyOnMissionStatusChanged();
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de MissionRedirected: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void simuleBounty(String timestamp,Mission mission) {
        System.out.println("Simulation d'un event Bounty");
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("timestamp",timestamp );
        node.put("event", "Bounty");
        node.putArray("Rewards"); // tableau vide
        node.put("PilotName", "");
        node.put("PilotName_Localised", " ? ");
        node.put("Target", "");
        node.put("Target_Localised", " ? ");
        node.put("TotalReward", 0);
        node.put("VictimFaction", mission.getTargetFaction());
        new BountyHandler().handle(node);
        if (notConsideredAsCompleted(mission)){
            simuleBounty(timestamp,mission);
        }
    }

    private boolean notConsideredAsCompleted(Mission mission) {
        return mission != null
                && mission.isShipMassacreActive()
                && (mission.getCurrentCount() != mission.getTargetCount());
    }
}