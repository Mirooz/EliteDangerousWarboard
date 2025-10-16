package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.dashboard.model.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.model.commander.Mission;
import be.mirooz.elitedangerous.dashboard.model.ships.DestroyedBountyShip;
import be.mirooz.elitedangerous.dashboard.model.ships.DestroyedConflictShip;
import be.mirooz.elitedangerous.dashboard.model.ships.Reward;
import be.mirooz.elitedangerous.dashboard.model.ships.ShipTarget;
import be.mirooz.elitedangerous.dashboard.util.comparator.MissionTimestampComparator;
import be.mirooz.elitedangerous.dashboard.model.registries.DestroyedShipsRegistery;
import be.mirooz.elitedangerous.dashboard.model.registries.MissionsRegistry;
import be.mirooz.elitedangerous.dashboard.util.DateUtil;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MissionService {

    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();
    private final MissionsRegistry missionsRegistry;
    private final ShipTargetService shipTargetService = ShipTargetService.getInstance();
    private final DestroyedShipsRegistery destroyedShipsRegistery;

    private MissionService() {
        this.missionsRegistry = MissionsRegistry.getInstance();
        this.destroyedShipsRegistery = DestroyedShipsRegistery.getInstance();
    }

    private static final MissionService INSTANCE = new MissionService();

    public static MissionService getInstance() {
        return INSTANCE;
    }

    public void updateBountyRewards(JsonNode jsonNode) {
        LocalDateTime timestamp = DateUtil.parseTimestamp(jsonNode.has("timestamp") ? jsonNode.get("timestamp").asText() : null);
        String shipName = jsonNode.has("Target_Localised") ? jsonNode.get("Target_Localised").asText() : jsonNode.has("Target") ? jsonNode.get("Target").asText() : "";
        String pilotName = jsonNode.has("PilotName_Localised") ? jsonNode.get("PilotName_Localised").asText() : "";
        int totalReward = jsonNode.has("TotalReward") ? jsonNode.get("TotalReward").asInt() : 0;
        String victimFaction = jsonNode.has("VictimFaction") ? jsonNode.get("VictimFaction").asText() : "";
        List<Reward> rewards = getRewards(jsonNode);
        System.out.println("VictimFaction: " + victimFaction + ", Reward: " + totalReward + ", Wanted : " + +rewards.size() + rewards);
        if (totalReward > 0) {
            DestroyedBountyShip destroyedShip = DestroyedBountyShip
                    .builder()
                    .destroyedTime(timestamp)
                    .shipName(shipName)
                    .pilotName(pilotName)
                    .bountyFaction(victimFaction)
                    .totalBountyReward(totalReward)
                    .rewards(rewards)
                    .build();
            destroyedShipsRegistery.addDestroyedShip(destroyedShip);
        }
    }

    public void updateFactionRewards(JsonNode jsonNode) {
        System.out.println(jsonNode);
        LocalDateTime timestamp = DateUtil.parseTimestamp(jsonNode.has("timestamp") ? jsonNode.get("timestamp").asText() : null);
        int totalReward = jsonNode.has("Reward") ? jsonNode.get("Reward").asInt() : 0;
        String victimFaction = jsonNode.has("VictimFaction") ? jsonNode.get("VictimFaction").asText() : "";
        List<Reward> rewards = new ArrayList<>();
        String rewardFaction = jsonNode.has("AwardingFaction") ? jsonNode.get("AwardingFaction").asText() : "";
        Reward reward = new Reward(rewardFaction, totalReward);
        rewards.add(reward);
        System.out.println("VictimFaction: " + victimFaction + ", Reward: " + totalReward + ", Wanted : " + +rewards.size() + rewards);
        if (totalReward > 0) {
            DestroyedConflictShip destroyedShip = DestroyedConflictShip
                    .builder()
                    .destroyedTime(timestamp)
                    .shipName(victimFaction)
                    .pilotName(victimFaction)
                    .bountyFaction(victimFaction)
                    .totalBountyReward(totalReward)
                    .rewards(rewards)
                    .build();
            destroyedShipsRegistery.addDestroyedShip(destroyedShip);
        }
    }


    private List<Reward> getRewards(JsonNode jsonNode) {
        List<Reward> rewards = new ArrayList<>();
        if (jsonNode.has("Rewards") && jsonNode.get("Rewards").isArray()) {
            for (JsonNode rewardNode : jsonNode.get("Rewards")) {
                String faction = rewardNode.has("Faction") ? rewardNode.get("Faction").asText() : null;
                int reward = rewardNode.has("Reward") ? rewardNode.get("Reward").asInt() : 0;
                rewards.add(new Reward(faction, reward));
            }
        }
        return rewards;
    }

    public void updateWantedKillCount(JsonNode jsonNode) {
        String victimFaction = jsonNode.has("VictimFaction") ? jsonNode.get("VictimFaction").asText() : "";
        if (missionsRegistry.getGlobalMissionMap().values().isEmpty()) {
            return;
        }
        List<Mission> eligibleMissions = new ArrayList<>();
        if ( isShip(jsonNode)) {
            if (isPirateShipKilled(jsonNode)) {
                System.out.println("Pirate bounty");
                eligibleMissions = getPirateShipMissions(victimFaction);
            }
            /*else if (isDeserteurShipKilled(jsonNode)) {
                System.out.println("Deserteur bounty");
                eligibleMissions = getDeserteurShipMissions(victimFaction);
            }*/
        }
//        else if (isOnFoot(jsonNode)){
//            //Pirate par defaut
//            eligibleMissions = getOnFootMissions(victimFaction);
//        }
        updateCurrentKillForMissions(eligibleMissions);
    }

    public void updatFactionKillCount(JsonNode jsonNode) {
        String victimFaction = jsonNode.has("VictimFaction") ? jsonNode.get("VictimFaction").asText() : "";
        if (missionsRegistry.getGlobalMissionMap().values().isEmpty()) {
            return;
        }
        List<Mission> eligibleMissions = getFactionShipMissions(victimFaction);
        updateCurrentKillForMissions(eligibleMissions);
    }

    private List<Mission> getDeserteurShipMissions(String victimFaction) {
        return getEligiblesMissions(victimFaction,
                Mission::isShipActiveDeserteurMassacreMission,
                Mission::isShipMassacre);
    }


    private boolean isDeserteurShipKilled(JsonNode jsonNode) {
        String pilotName_Localised = jsonNode.has("PilotName_Localised") ? jsonNode.get("PilotName_Localised").asText() : "";
        ShipTarget shipTarget = getShipTarget(jsonNode, pilotName_Localised);
        if (shipTarget == null) return false;
        return shipTarget.isDeserteur();

    }

    private ShipTarget getShipTarget(JsonNode jsonNode, String pilotName_Localised) {
        ShipTarget shipTarget = shipTargetService.getTarget(pilotName_Localised);
        if (shipTarget == null) {
            System.out.println("[Error bounty] ship not targeted");
            System.out.println(jsonNode);
        }
        return shipTarget;
    }

    private void updateCurrentKillForMissions(List<Mission> eligibleMissions) {
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

    private boolean isPirateShipKilled(JsonNode jsonNode) {
        return true;
        /*TODO Les deserteur ne sont pas dinstinguable*/
       /*
        String pilotName_Localised = jsonNode.has("PilotName_Localised") ? jsonNode.get("PilotName_Localised").asText() : "";
        ShipTarget shipTarget = getShipTarget(jsonNode, pilotName_Localised);
        if (shipTarget == null) return false;
        return shipTarget.isPirate();*/
    }

    private boolean isShip(JsonNode jsonNode) {
        String target = jsonNode.hasNonNull("Target") ? jsonNode.get("Target").asText().toLowerCase().trim() : "";
        return !target.contains("suitai") && !target.contains("skimmerdrone");
    }
    private boolean isOnFoot(JsonNode jsonNode){
        String target = jsonNode.hasNonNull("Target") ? jsonNode.get("Target").asText().toLowerCase().trim() : "";
        return target.contains("suitai");
    }

    private List<Mission> getOnFootMissions(String victimFaction) {
        return getEligiblesMissions(victimFaction,
                Mission::isOnFootActiveMassacreMission,
                Mission::isOnFootMassacre);
    }

    private List<Mission> getPirateShipMissions(String victimFaction) {
        return getEligiblesMissions(victimFaction,
                Mission::isShipActivePirateMassacreMission,
                Mission::isShipMassacre);
    }

    private List<Mission> getFactionShipMissions(String victimFaction) {
        return getEligiblesMissions(victimFaction,
                Mission::isShipActiveFactionConflictMission,
                Mission::isShipMassacre);
    }

    @SafeVarargs
    private List<Mission> getEligiblesMissions(String victimFaction, Predicate<Mission>... extraFilters) {
        Stream<Mission> stream = missionsRegistry.getGlobalMissionMap().values().stream()
                .filter(mission -> mission.getTargetFaction() != null)
                .filter(mission -> victimFaction.equals(mission.getTargetFaction()))
                .filter(mission -> mission.getDestinationSystem() == null ||
                        commanderStatus.getCurrentStarSystem().equals(mission.getDestinationSystem()))
                .filter(mission -> mission.getTargetCountLeft() > 0);

        // Application des filtres supplémentaires passés en paramètre
        for (Predicate<Mission> filter : extraFilters) {
            stream = stream.filter(filter);
        }

        return stream.toList();
    }

}
