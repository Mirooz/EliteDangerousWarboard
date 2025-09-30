package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.dashboard.model.Mission;
import be.mirooz.elitedangerous.dashboard.model.MissionStatus;
import be.mirooz.elitedangerous.dashboard.model.MissionType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MissionService {

    private MissionService(){
    }
    private static final MissionService INSTANCE = new MissionService();
    public static MissionService getInstance() { return INSTANCE; }

    public boolean updateKillsCount(Collection<Mission> missions, String victimFaction) {
        if (missions.isEmpty()){
            return false;
        }
        List<Mission> eligibleMissions = missions.stream()
                .filter(mission -> mission.getStatus() == MissionStatus.ACTIVE)
                .filter(mission -> mission.getType() == MissionType.MASSACRE)
                .filter(mission -> mission.getTargetFaction() != null)
                .filter(mission -> victimFaction.equals(mission.getTargetFaction()))
                .filter(mission -> mission.getTargetCountLeft() >0)
                .toList();

        System.out.println("Missions éligibles trouvées: " + eligibleMissions.size());

        if (eligibleMissions.isEmpty()) {
            return true;
        }

        // Grouper par faction source et trier par date d'acceptation (plus ancienne en premier)
        Map<String, List<Mission>> missionsBySourceFaction = eligibleMissions.stream()
                .collect(Collectors.groupingBy(Mission::getFaction));

        // Pour chaque faction source, prendre la mission la plus ancienne
        for (Map.Entry<String, List<Mission>> entry : missionsBySourceFaction.entrySet()) {
            String sourceFaction = entry.getKey();
            List<Mission> factionMissions = entry.getValue();

            // Trier par date d'acceptation (plus ancienne en premier)
            factionMissions.sort((m1, m2) -> {
                if (m1.getAcceptedTime() == null && m2.getAcceptedTime() == null) return 0;
                if (m1.getAcceptedTime() == null) return 1;
                if (m2.getAcceptedTime() == null) return -1;
                return m1.getAcceptedTime().compareTo(m2.getAcceptedTime());
            });

            // Prendre la mission la plus ancienne
            Mission oldestMission = factionMissions.get(0);

            // Incrémenter le compteur
            int oldCount = oldestMission.getCurrentCount();
            int newCount = oldCount + 1;
            oldestMission.setCurrentCount(newCount);

            System.out.println("Mission " + oldestMission.getId() + " (" + sourceFaction + ") : " +
                    oldCount + " -> " + newCount + "/" + oldestMission.getTargetCount());

            // Si la mission atteint le nombre de kills requis, la marquer comme en attente
            if (newCount >= oldestMission.getTargetCount()) {
                System.out.println("Mission " + oldestMission.getId() + " en attente de completion");
                // La mission reste ACTIVE mais sera affichée en bleu (en attente)
                // Le statut reste ACTIVE pour éviter les conflits avec MissionCompleted
            }
        }
        return false;
    }

}
