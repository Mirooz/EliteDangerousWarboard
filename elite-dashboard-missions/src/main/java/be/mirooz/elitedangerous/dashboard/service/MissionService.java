package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.dashboard.comparator.MissionTimestampComparator;
import be.mirooz.elitedangerous.dashboard.model.*;
import be.mirooz.elitedangerous.dashboard.model.enums.MissionStatus;
import be.mirooz.elitedangerous.dashboard.model.enums.MissionType;
import be.mirooz.elitedangerous.dashboard.util.DateUtil;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MissionService {

    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();
    private final MissionsList missionsList;
    private final DestroyedShipsList destroyedShipsList;

    private MissionService() {
        this.missionsList = MissionsList.getInstance();
        this.destroyedShipsList = DestroyedShipsList.getInstance();
    }

    private static final MissionService INSTANCE = new MissionService();

    public static MissionService getInstance() {
        return INSTANCE;
    }

    public void updateTargetRewards(JsonNode jsonNode) {
        LocalDateTime timestamp = DateUtil.parseTimestamp(jsonNode.has("timestamp") ? jsonNode.get("timestamp").asText() : null);
        String shipName = jsonNode.has("Target_Localised") ? jsonNode.get("Target_Localised").asText() : jsonNode.has("Target") ? jsonNode.get("Target").asText() : "";
        String pilotName = jsonNode.has("PilotName_Localised") ? jsonNode.get("PilotName_Localised").asText() : "";
        int totalReward = jsonNode.has("TotalReward") ? jsonNode.get("TotalReward").asInt() : 0;
        String victimFaction = jsonNode.has("VictimFaction") ? jsonNode.get("VictimFaction").asText() : "";
        List<Reward> rewards = new ArrayList<>();
        if (jsonNode.has("Rewards") && jsonNode.get("Rewards").isArray()) {
            for (JsonNode rewardNode : jsonNode.get("Rewards")) {
                String faction = rewardNode.has("Faction") ? rewardNode.get("Faction").asText() : null;
                int reward = rewardNode.has("Reward") ? rewardNode.get("Reward").asInt() : 0;
                rewards.add(new Reward(faction, reward));
            }
        }
        if (totalReward > 0) {
            DestroyedShip destroyedShip = DestroyedShip
                    .builder()
                    .destroyedTime(timestamp)
                    .shipName(shipName)
                    .pilotName(pilotName)
                    .bountyFaction(victimFaction)
                    .totalBountyReward(totalReward)
                    .rewards(rewards)
                    .build();
            destroyedShipsList.addDestroyedShip(destroyedShip);
        }
    }

    public void updateKillsCount(JsonNode jsonNode) {

        String victimFaction = jsonNode.has("VictimFaction") ? jsonNode.get("VictimFaction").asText() : "";
        int totalReward = jsonNode.has("TotalReward") ? jsonNode.get("TotalReward").asInt() : 0;
        List<Reward> rewards = new ArrayList<>();
        if (jsonNode.has("Rewards") && jsonNode.get("Rewards").isArray()) {
            for (JsonNode rewardNode : jsonNode.get("Rewards")) {
                String faction = rewardNode.has("Faction") ? rewardNode.get("Faction").asText() : null;
                int reward = rewardNode.has("Reward") ? rewardNode.get("Reward").asInt() : 0;
                rewards.add(new Reward(faction, reward));
            }
        }
        System.out.println("VictimFaction: " + victimFaction + ", Reward: " + totalReward + ", Wanted : " + +rewards.size() + rewards);

        if (missionsList.getGlobalMissionMap().values().isEmpty()) {
            return;
        }
        List<Mission> eligibleMissions = missionsList.getGlobalMissionMap().values().stream()
                .filter(Mission::isActivePirateMission)
                .filter(mission -> mission.getType() == MissionType.MASSACRE)
                .filter(mission -> mission.getTargetFaction() != null)
                .filter(mission -> victimFaction.equals(mission.getTargetFaction()))
                .filter(mission -> commanderStatus.getCurrentStarSystemString().equals(mission.getDestinationSystem()))
                .filter(mission -> mission.getTargetCountLeft() > 0)
                .toList();


        if (eligibleMissions.isEmpty()) {
            return;
        }

        // Grouper par faction source et trier par date d'acceptation (plus ancienne en premier)
        Map<String, List<Mission>> missionsBySourceFaction = eligibleMissions.stream()
                .collect(Collectors.groupingBy(Mission::getFaction));

        // Pour chaque faction source, prendre la mission la plus ancienne
        for (Map.Entry<String, List<Mission>> entry : missionsBySourceFaction.entrySet()) {
            String sourceFaction = entry.getKey();
            List<Mission> factionMissions = entry.getValue();

            // Trier par date d'acceptation (plus ancienne en premier)
            factionMissions.sort(new MissionTimestampComparator(true));

            // Prendre la mission la plus ancienne
            Mission oldestMission = factionMissions.get(0);

            // Incrémenter le compteur
            int oldCount = oldestMission.getCurrentCount();
            int newCount = oldCount + 1;
            oldestMission.setCurrentCount(newCount);

            System.out.println("Mission " + oldestMission.getId() + " (" + sourceFaction + ") : " +
                    oldCount + " -> " + newCount + "/" + oldestMission.getTargetCount());
            if (newCount >= oldestMission.getTargetCount()) {
                System.out.println("Mission " + oldestMission.getId() + " en attente de completion");
            }

        }

    }

}
