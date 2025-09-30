package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.dashboard.model.Mission;
import be.mirooz.elitedangerous.dashboard.model.MissionStatus;
import be.mirooz.elitedangerous.dashboard.model.MissionType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service pour gérer les missions Elite Dangerous
 */
public class MissionService {
    
    private JournalService journalService;
    
    /**
     * Récupère la liste des missions actives
     * @return Liste des missions actives
     */
    public List<Mission> getActiveMissions() {
        // Essayer de lire les données réelles des journaux
        try {
            if (journalService == null) {
                journalService = new JournalService();
            }
            List<Mission> realMissions = journalService.getMissionsFromLastWeek();
            
            // Si on a des missions réelles, les utiliser
            if (!realMissions.isEmpty()) {
                return realMissions;
            }
        } catch (Exception e) {
            System.err.println("Impossible de lire les journaux, utilisation des données de test: " + e.getMessage());
        }
        
        // Sinon, retourner des données de test
        return new ArrayList<>();
    }
    
    /**
     * Récupère le nom du commandant
     * @return Nom du commandant ou null si non identifié
     */
    public String getCommanderName() {
        if (journalService == null) {
            journalService = new JournalService();
        }
        return journalService.getCommanderName();
    }
    
    /**
     * Récupère le système actuel
     * @return Nom du système ou null si non identifié
     */
    public String getCurrentSystem() {
        if (journalService == null) {
            journalService = new JournalService();
        }
        return journalService.getCurrentSystem();
    }
    
    /**
     * Récupère la station actuelle
     * @return Nom de la station ou null si non identifiée
     */
    public String getCurrentStation() {
        if (journalService == null) {
            journalService = new JournalService();
        }
        return journalService.getCurrentStation();
    }
    
    /**
     * Récupère le vaisseau actuel
     * @return Nom du vaisseau ou null si non identifié
     */
    public String getCurrentShip() {
        if (journalService == null) {
            journalService = new JournalService();
        }
        return journalService.getCurrentShip();
    }
    
    /**
     * Génère des missions de test pour la démonstration
     * @return Liste de missions de test
     */
    private List<Mission> generateTestMissions() {
        List<Mission> missions = new ArrayList<>();
        
        // Mission 1: Transport de courrier
        Mission mission1 = new Mission();
        mission1.setId("M001");
        mission1.setName("Transport de courrier urgent");
        mission1.setDescription("Transportez des documents confidentiels vers la station voisine");
        mission1.setType(MissionType.COURIER);
        mission1.setStatus(MissionStatus.ACTIVE);
        mission1.setFaction("Fédération Galactique");
        mission1.setOrigin("Sol - Abraham Lincoln");
        mission1.setDestination("Proxima Centauri - New Horizons");
        mission1.setReward(125000);
        mission1.setInfluence(2);
        mission1.setReputation(1);
        mission1.setExpiry(LocalDateTime.now().plusHours(6));
        missions.add(mission1);
        
        // Mission 2: Chasse aux primes
        Mission mission2 = new Mission();
        mission2.setId("M002");
        mission2.setName("Élimination de pirate");
        mission2.setDescription("Éliminez le pirate notoire 'Red Viper' dans le système Lave");
        mission2.setType(MissionType.ASSASSINATION);
        mission2.setStatus(MissionStatus.ACTIVE);
        mission2.setFaction("Alliance des Systèmes Indépendants");
        mission2.setOrigin("Lave - Lave Station");
        mission2.setDestination("Lave - Zone de ressources");
        mission2.setReward(450000);
        mission2.setInfluence(3);
        mission2.setReputation(2);
        mission2.setExpiry(LocalDateTime.now().plusDays(2));
        mission2.setTargetCount(1);
        mission2.setCurrentCount(0);
        missions.add(mission2);
        
        // Mission 3: Livraison de marchandises
        Mission mission3 = new Mission();
        mission3.setId("M003");
        mission3.setName("Livraison de minerais rares");
        mission3.setDescription("Transportez 50 tonnes de Painite vers la station de raffinage");
        mission3.setType(MissionType.DELIVERY);
        mission3.setStatus(MissionStatus.ACTIVE);
        mission3.setFaction("Empire Galactique");
        mission3.setOrigin("Shinrarta Dezhra - Jameson Memorial");
        mission3.setDestination("Eravate - Cleve Hub");
        mission3.setReward(320000);
        mission3.setInfluence(2);
        mission3.setReputation(1);
        mission3.setExpiry(LocalDateTime.now().plusHours(12));
        mission3.setCommodity("Painite");
        mission3.setCommodityCount(50);
        missions.add(mission3);
        
        // Mission 4: Transport de passagers
        Mission mission4 = new Mission();
        mission4.setId("M004");
        mission4.setName("Voyage de luxe");
        mission4.setDescription("Transportez des passagers VIP vers une destination de luxe");
        mission4.setType(MissionType.PASSENGER);
        mission4.setStatus(MissionStatus.ACTIVE);
        mission4.setFaction("Fédération Galactique");
        mission4.setOrigin("Sol - Abraham Lincoln");
        mission4.setDestination("Rhea - Rhea Station");
        mission4.setReward(180000);
        mission4.setInfluence(1);
        mission4.setReputation(1);
        mission4.setExpiry(LocalDateTime.now().plusDays(1));
        missions.add(mission4);
        
        // Mission 5: Massacre de pirates
        Mission mission5 = new Mission();
        mission5.setId("M005");
        mission5.setName("Nettoyage de la zone");
        mission5.setDescription("Éliminez 15 pirates dans la zone de ressources de Eravate");
        mission5.setType(MissionType.MASSACRE);
        mission5.setStatus(MissionStatus.ACTIVE);
        mission5.setFaction("Alliance des Systèmes Indépendants");
        mission5.setOrigin("Eravate - Cleve Hub");
        mission5.setDestination("Eravate - Zone de ressources");
        mission5.setReward(750000);
        mission5.setInfluence(4);
        mission5.setReputation(3);
        mission5.setExpiry(LocalDateTime.now().plusDays(3));
        mission5.setTargetCount(15);
        mission5.setCurrentCount(8);
        mission5.setTargetFaction("Pirates de l'Espace");
        missions.add(mission5);
        
        // Mission 9: Massacre de pirates - Fédération
        Mission mission9 = new Mission();
        mission9.setId("M009");
        mission9.setName("Opération de nettoyage");
        mission9.setDescription("Éliminez 20 pirates dans le système Sol");
        mission9.setType(MissionType.MASSACRE);
        mission9.setStatus(MissionStatus.ACTIVE);
        mission9.setFaction("Fédération Galactique");
        mission9.setOrigin("Sol - Abraham Lincoln");
        mission9.setDestination("Sol - Zone de ressources");
        mission9.setReward(950000);
        mission9.setInfluence(5);
        mission9.setReputation(4);
        mission9.setExpiry(LocalDateTime.now().plusDays(2));
        mission9.setTargetCount(20);
        mission9.setCurrentCount(12);
        mission9.setTargetFaction("Pirates de l'Espace");
        missions.add(mission9);
        
        // Mission 10: Massacre de pirates - Empire
        Mission mission10 = new Mission();
        mission10.setId("M010");
        mission10.setName("Purification impériale");
        mission10.setDescription("Éliminez 25 pirates dans le système Achenar");
        mission10.setType(MissionType.MASSACRE);
        mission10.setStatus(MissionStatus.ACTIVE);
        mission10.setFaction("Empire Galactique");
        mission10.setOrigin("Achenar - Capitol");
        mission10.setDestination("Achenar - Zone de ressources");
        mission10.setReward(1200000);
        mission10.setInfluence(6);
        mission10.setReputation(5);
        mission10.setExpiry(LocalDateTime.now().plusDays(4));
        mission10.setTargetCount(25);
        mission10.setCurrentCount(18);
        mission10.setTargetFaction("Pirates de l'Espace");
        missions.add(mission10);
        
        // Mission 11: Massacre de pirates - Alliance
        Mission mission11 = new Mission();
        mission11.setId("M011");
        mission11.setName("Défense de l'Alliance");
        mission11.setDescription("Éliminez 18 pirates dans le système Alioth");
        mission11.setType(MissionType.MASSACRE);
        mission11.setStatus(MissionStatus.ACTIVE);
        mission11.setFaction("Alliance des Systèmes Indépendants");
        mission11.setOrigin("Alioth - Irkutsk");
        mission11.setDestination("Alioth - Zone de ressources");
        mission11.setReward(850000);
        mission11.setInfluence(4);
        mission11.setReputation(3);
        mission11.setExpiry(LocalDateTime.now().plusDays(3));
        mission11.setTargetCount(18);
        mission11.setCurrentCount(7);
        mission11.setTargetFaction("Pirates de l'Espace");
        missions.add(mission11);
        
        // Mission 12: Massacre de pirates - Fédération
        Mission mission12 = new Mission();
        mission12.setId("M012");
        mission12.setName("Sécurisation du secteur");
        mission12.setDescription("Éliminez 30 pirates dans le système Eravate");
        mission12.setType(MissionType.MASSACRE);
        mission12.setStatus(MissionStatus.ACTIVE);
        mission12.setFaction("Fédération Galactique");
        mission12.setOrigin("Eravate - Cleve Hub");
        mission12.setDestination("Eravate - Zone de ressources");
        mission12.setReward(1500000);
        mission12.setInfluence(7);
        mission12.setReputation(6);
        mission12.setExpiry(LocalDateTime.now().plusDays(5));
        mission12.setTargetCount(30);
        mission12.setCurrentCount(22);
        mission12.setTargetFaction("Pirates de l'Espace");
        missions.add(mission12);
        
        // Mission 6: Exploration
        Mission mission6 = new Mission();
        mission6.setId("M006");
        mission6.setName("Exploration du secteur inconnu");
        mission6.setDescription("Explorez et cartographiez les systèmes inexplorés du secteur NGC 7822");
        mission6.setType(MissionType.EXPLORATION);
        mission6.setStatus(MissionStatus.ACTIVE);
        mission6.setFaction("Fédération Galactique");
        mission6.setOrigin("Sol - Abraham Lincoln");
        mission6.setDestination("NGC 7822 - Systèmes inexplorés");
        mission6.setReward(280000);
        mission6.setInfluence(2);
        mission6.setReputation(2);
        mission6.setExpiry(LocalDateTime.now().plusDays(7));
        missions.add(mission6);
        
        // Mission 7: Récupération
        Mission mission7 = new Mission();
        mission7.setId("M007");
        mission7.setName("Récupération de données");
        mission7.setDescription("Récupérez des données perdues dans les épaves du système HIP 20277");
        mission7.setType(MissionType.SALVAGE);
        mission7.setStatus(MissionStatus.ACTIVE);
        mission7.setFaction("Empire Galactique");
        mission7.setOrigin("HIP 20277 - Fabian City");
        mission7.setDestination("HIP 20277 - Sites d'épaves");
        mission7.setReward(195000);
        mission7.setInfluence(2);
        mission7.setReputation(1);
        mission7.setExpiry(LocalDateTime.now().plusHours(18));
        mission7.setTargetCount(5);
        mission7.setCurrentCount(3);
        missions.add(mission7);
        
        // Mission 8: Scan
        Mission mission8 = new Mission();
        mission8.setId("M008");
        mission8.setName("Scan de surface");
        mission8.setDescription("Scannez les installations de surface sur la planète A 1");
        mission8.setType(MissionType.SCAN);
        mission8.setStatus(MissionStatus.ACTIVE);
        mission8.setFaction("Alliance des Systèmes Indépendants");
        mission8.setOrigin("Eravate - Cleve Hub");
        mission8.setDestination("Eravate - A 1");
        mission8.setReward(95000);
        mission8.setInfluence(1);
        mission8.setReputation(1);
        mission8.setExpiry(LocalDateTime.now().plusHours(8));
        missions.add(mission8);
        
        return missions;
    }
    
    /**
     * Récupère une mission par son ID
     * @param missionId ID de la mission
     * @return Mission correspondante ou null
     */
    public Mission getMissionById(String missionId) {
        return getActiveMissions().stream()
                .filter(mission -> mission.getId().equals(missionId))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Met à jour le statut d'une mission
     * @param missionId ID de la mission
     * @param status Nouveau statut
     * @return true si la mise à jour a réussi
     */
    public boolean updateMissionStatus(String missionId, MissionStatus status) {
        // Dans une vraie application, cela ferait appel à l'API
        return true;
    }
}
