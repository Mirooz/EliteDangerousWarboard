package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.dashboard.model.Mission;
import be.mirooz.elitedangerous.dashboard.model.MissionStatus;
import be.mirooz.elitedangerous.dashboard.model.MissionType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service pour lire et parser les fichiers Journal Elite Dangerous
 */
public class JournalService {
    
    private static final String JOURNAL_PATH = "C:\\Users\\ewen_\\Saved Games\\Frontier Developments\\Elite Dangerous";
    private static final String JOURNAL_PREFIX = "Journal.";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Récupère toutes les missions des 7 derniers jours
     */
    public List<Mission> getMissionsFromLastWeek() {
        try {
            // Utiliser la nouvelle méthode qui traite tous les fichiers et met à jour les missions
            return parseAllJournalFiles();
        } catch (Exception e) {
            System.err.println("Erreur lors de la lecture des journaux: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * Récupère les fichiers Journal des 7 derniers jours
     */
    private List<File> getJournalFilesFromLastWeek() throws IOException {
        List<File> journalFiles = new ArrayList<>();
        Path journalDir = Paths.get(JOURNAL_PATH);
        
        if (!Files.exists(journalDir)) {
            System.err.println("Dossier Journal introuvable: " + JOURNAL_PATH);
            return journalFiles;
        }
        
        LocalDate oneWeekAgo = LocalDate.now().minusDays(7);
        
        try (Stream<Path> paths = Files.list(journalDir)) {
            paths.filter(path -> path.getFileName().toString().startsWith(JOURNAL_PREFIX))
                 .filter(path -> isFileFromLastWeek(path, oneWeekAgo))
                 .sorted((p1, p2) -> p2.getFileName().toString().compareTo(p1.getFileName().toString()))
                 .forEach(path -> journalFiles.add(path.toFile()));
        }
        
        return journalFiles;
    }
    
    /**
     * Vérifie si un fichier Journal est de la semaine dernière
     */
    private boolean isFileFromLastWeek(Path filePath, LocalDate oneWeekAgo) {
        try {
            String fileName = filePath.getFileName().toString();
            // Format: Journal.2025-09-19T121254.01
            String datePart = fileName.substring(JOURNAL_PREFIX.length(), JOURNAL_PREFIX.length() + 10);
            LocalDate fileDate = LocalDate.parse(datePart);
            return !fileDate.isBefore(oneWeekAgo);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Parse tous les fichiers Journal et met à jour les missions existantes
     */
    private List<Mission> parseAllJournalFiles() {
        Map<String, Mission> globalMissionMap = new HashMap<>();
        
        try {
            List<File> journalFiles = getJournalFilesFromLastWeek();
            
            // Traiter les fichiers dans l'ordre chronologique (plus ancien en premier)
            // Les fichiers sont déjà triés par date décroissante, on les inverse
            Collections.reverse(journalFiles);
            for (File journalFile : journalFiles) {
                Map<String, Mission> fileMissionMap = new HashMap<>();
                
                try {
                    List<String> lines = Files.readAllLines(journalFile.toPath());
                    
                    for (String line : lines) {
                        try {
                            JsonNode jsonNode = objectMapper.readTree(line);
                            String event = jsonNode.get("event").asText();
                            
                            switch (event) {
                                case "MissionAccepted":
                                    handleMissionAccepted(jsonNode, fileMissionMap);
                                    break;
                                case "MissionCompleted":
                                    handleMissionCompleted(jsonNode, fileMissionMap);
                                    break;
                                case "MissionAbandoned":
                                    handleMissionAbandoned(jsonNode, fileMissionMap);
                                    break;
                                case "MissionRedirected":
                                    handleMissionRedirected(jsonNode, fileMissionMap);
                                    break;
                                case "Bounty":
                                    handleBounty(jsonNode, fileMissionMap,globalMissionMap);
                                    break;
                                case "FactionKillBond":
                                    handleFactionKillBond(jsonNode, fileMissionMap, globalMissionMap);
                                    break;
                                case "MissionProgress":
                                    handleMissionProgress(jsonNode, fileMissionMap);
                                    break;
                            }
                        } catch (Exception e) {
                            // Ignorer les lignes malformées
                        }
                    }

                    // Fusionner les missions de ce fichier avec le map global
                    for (Map.Entry<String, Mission> entry : fileMissionMap.entrySet()) {
                        String missionId = entry.getKey();
                        Mission newMission = entry.getValue();

                        if (globalMissionMap.containsKey(missionId)) {
                            // Mettre à jour la mission existante avec toutes les informations
                            Mission existingMission = globalMissionMap.get(missionId);
                            existingMission.setStatus(newMission.getStatus());
                            existingMission.setCurrentCount(newMission.getCurrentCount());
                            existingMission.setTargetCount(newMission.getTargetCount());
                            existingMission.setTargetFaction(newMission.getTargetFaction());
                            existingMission.setTargetSystem(newMission.getTargetSystem());
                            existingMission.setAcceptedTime(newMission.getAcceptedTime());
                            existingMission.setExpiry(newMission.getExpiry());
                            existingMission.setReward(newMission.getReward());
                            // Mettre à jour toutes les autres propriétés importantes
                        } else {
                            // Ajouter la nouvelle mission
                            globalMissionMap.put(missionId, newMission);
                        }
                    }
                    
                    // Traiter les événements qui peuvent mettre à jour des missions existantes
                    // même si elles n'ont pas été créées dans ce fichier
                    try {
                        List<String> eventLines = Files.readAllLines(journalFile.toPath());
                        
                        for (String line : eventLines) {
                            try {
                                JsonNode jsonNode = objectMapper.readTree(line);
                                String event = jsonNode.get("event").asText();
                                
                                if ("MissionCompleted".equals(event) || "MissionAbandoned".equals(event)) {
                                    String missionId = jsonNode.get("MissionID").asText();
                                    if (globalMissionMap.containsKey(missionId)) {
                                        Mission mission = globalMissionMap.get(missionId);
                                        if ("MissionCompleted".equals(event)) {
                                            mission.setStatus(MissionStatus.COMPLETED);
                                            if (mission.getTargetCount() == 0 && jsonNode.has("KillCount")) {
                                                mission.setTargetCount(jsonNode.get("KillCount").asInt());
                                            }
                                            mission.setCurrentCount(mission.getTargetCount());
                                        } else if ("MissionAbandoned".equals(event)) {
                                            mission.setStatus(MissionStatus.FAILED);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                // Ignorer les lignes malformées
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Erreur lors de la relecture du fichier " + journalFile.getName() + ": " + e.getMessage());
                    }
                    
                } catch (IOException e) {
                    System.err.println("Erreur lors de la lecture du fichier " + journalFile.getName() + ": " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la lecture des journaux: " + e.getMessage());
        }
        
        return new ArrayList<>(globalMissionMap.values());
    }
    
    /**
     * Traite l'événement MissionAccepted
     */
    private void handleMissionAccepted(JsonNode jsonNode, Map<String, Mission> missionMap) {
        try {
            String missionId = jsonNode.get("MissionID").asText();
            String missionName = jsonNode.get("Name").asText();
            String faction = jsonNode.get("Faction").asText();
            String targetFaction = jsonNode.has("TargetFaction") ? jsonNode.get("TargetFaction").asText() : null;
            String targetSystem = jsonNode.has("TargetSystem") ? jsonNode.get("TargetSystem").asText() : null;
            
            // Essayer différents champs pour le nombre de kills requis
            int targetCount = 0;
            if (jsonNode.has("TargetCount")) {
                targetCount = jsonNode.get("TargetCount").asInt();
            } else if (jsonNode.has("KillCount")) {
                targetCount = jsonNode.get("KillCount").asInt();
            } else if (jsonNode.has("Count")) {
                targetCount = jsonNode.get("Count").asInt();
            } else if (jsonNode.has("Amount")) {
                targetCount = jsonNode.get("Amount").asInt();
            }
            int reward = jsonNode.has("Reward") ? jsonNode.get("Reward").asInt() : 0;
            String timestamp = jsonNode.get("timestamp").asText();
            
            // Récupérer la date d'expiration depuis le journal
            LocalDateTime expiryTime = null;
            if (jsonNode.has("Expiry")) {
                expiryTime = parseTimestamp(jsonNode.get("Expiry").asText());
            } else if (jsonNode.has("Deadline")) {
                expiryTime = parseTimestamp(jsonNode.get("Deadline").asText());
            } else {
                // Fallback: 7 jours par défaut
                expiryTime = parseTimestamp(timestamp).plusDays(7);
            }
            
            Mission mission = new Mission();
            mission.setId(missionId);
            mission.setName(missionName);
            mission.setFaction(faction);
            mission.setTargetFaction(targetFaction);
            mission.setTargetSystem(targetSystem);
            mission.setTargetCount(targetCount);
            mission.setCurrentCount(0);
            mission.setReward(reward);
            mission.setStatus(MissionStatus.ACTIVE);
            mission.setType(determineMissionType(missionName));
            mission.setAcceptedTime(parseTimestamp(timestamp));
            mission.setExpiry(expiryTime);
            
            missionMap.put(missionId, mission);
            
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de MissionAccepted: " + e.getMessage());
        }
    }
    
    /**
     * Traite l'événement MissionCompleted
     */
    private void handleMissionCompleted(JsonNode jsonNode, Map<String, Mission> missionMap) {
        try {
            String missionId = jsonNode.get("MissionID").asText();
            Mission mission = missionMap.get(missionId);
            if (mission != null) {
                mission.setStatus(MissionStatus.COMPLETED);
                // Pour les missions complétées, mettre le compteur au maximum
                // Si targetCount est 0, essayer de le récupérer depuis l'événement
                if (mission.getTargetCount() == 0 && jsonNode.has("KillCount")) {
                    mission.setTargetCount(jsonNode.get("KillCount").asInt());
                }
                mission.setCurrentCount(mission.getTargetCount());
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de MissionCompleted: " + e.getMessage());
        }
    }
    
    /**
     * Traite l'événement MissionAbandoned
     */
    private void handleMissionAbandoned(JsonNode jsonNode, Map<String, Mission> missionMap) {
        try {
            String missionId = jsonNode.get("MissionID").asText();
            Mission mission = missionMap.get(missionId);
            if (mission != null) {
                mission.setStatus(MissionStatus.FAILED);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de MissionAbandoned: " + e.getMessage());
        }
    }
    
    /**
     * Traite l'événement MissionRedirected
     */
    private void handleMissionRedirected(JsonNode jsonNode, Map<String, Mission> missionMap) {
        try {
            String missionId = jsonNode.get("MissionID").asText();
            Mission mission = missionMap.get(missionId);
            if (mission != null) {
                // Mettre à jour la destination si nécessaire
                if (jsonNode.has("NewDestinationStation")) {
                    mission.setDestination(jsonNode.get("NewDestinationStation").asText());
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de MissionRedirected: " + e.getMessage());
        }
    }
    
    /**
     * Traite l'événement Bounty (kill de pirate)
     */
    private void handleBounty(JsonNode jsonNode, Map<String, Mission> missionMap, Map<String, Mission> globalMissionMap) {
        try {
            String victimFaction = jsonNode.has("VictimFaction") ? jsonNode.get("VictimFaction").asText() : "";
            int reward = jsonNode.has("TotalReward") ? jsonNode.get("TotalReward").asInt() : 0;
            
            System.out.println("Bounty event - VictimFaction: " + victimFaction + ", Reward: " + reward);
            
            // Trouver toutes les missions actives de massacre pour cette faction cible
            if (updateKillsCount(missionMap,globalMissionMap, victimFaction)) return; // Aucune mission éligible
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de Bounty: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean updateKillsCount(Map<String, Mission> missionMap, Map<String, Mission> globalMissionMap, String victimFaction) {
        List<Mission> eligibleMissions = Stream
                .concat(missionMap.values().stream(), globalMissionMap.values().stream())
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

    /**
     * Traite l'événement FactionKillBond (kill de faction)
     */
    private void handleFactionKillBond(JsonNode jsonNode, Map<String, Mission> missionMap, Map<String, Mission> globalMssionMap) {
        try {
            String victimFaction = jsonNode.has("VictimFaction") ? jsonNode.get("VictimFaction").asText() : "";
            int reward = jsonNode.has("Reward") ? jsonNode.get("Reward").asInt() : 0;
            
            System.out.println("FactionKillBond event - VictimFaction: " + victimFaction + ", Reward: " + reward);
            
            // Utiliser la même logique que handleBounty
            if (updateKillsCount(missionMap,globalMssionMap, victimFaction)) return; // Aucune mission éligible
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de FactionKillBond: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Traite l'événement MissionProgress (progression de mission)
     */
    private void handleMissionProgress(JsonNode jsonNode, Map<String, Mission> missionMap) {
        try {
            String missionId = jsonNode.get("MissionID").asText();
            Mission mission = missionMap.get(missionId);
            if (mission != null && mission.getStatus() == MissionStatus.ACTIVE) {
                // Mettre à jour la progression si disponible
                if (jsonNode.has("Progress")) {
                    int progress = jsonNode.get("Progress").asInt();
                    mission.setCurrentCount(Math.min(progress, mission.getTargetCount()));
                }
                // Essayer de récupérer le nombre total requis si pas encore défini
                if (mission.getTargetCount() == 0 && jsonNode.has("KillCount")) {
                    mission.setTargetCount(jsonNode.get("KillCount").asInt());
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de MissionProgress: " + e.getMessage());
        }
    }
    
    /**
     * Détermine le type de mission basé sur le nom
     */
    private MissionType determineMissionType(String missionName) {
        String name = missionName.toLowerCase();
        
        if (name.contains("massacre") || name.contains("kill")) {
            return MissionType.MASSACRE;
        } else if (name.contains("assassination") || name.contains("assassinat")) {
            return MissionType.ASSASSINATION;
        } else if (name.contains("delivery") || name.contains("livraison")) {
            return MissionType.DELIVERY;
        } else if (name.contains("courier") || name.contains("courrier")) {
            return MissionType.COURIER;
        } else if (name.contains("passenger") || name.contains("passager")) {
            return MissionType.PASSENGER;
        } else if (name.contains("bounty") || name.contains("prime")) {
            return MissionType.BOUNTY_HUNTING;
        } else if (name.contains("exploration") || name.contains("exploration")) {
            return MissionType.EXPLORATION;
        } else if (name.contains("salvage") || name.contains("récupération")) {
            return MissionType.SALVAGE;
        } else if (name.contains("scan")) {
            return MissionType.SCAN;
        } else if (name.contains("mining")) {
            return MissionType.MINING;
        } else if (name.contains("smuggling") || name.contains("contrebande")) {
            return MissionType.SMUGGLING;
        } else if (name.contains("trading") || name.contains("commerce")) {
            return MissionType.TRADING;
        } else {
            return MissionType.COMBAT; // Par défaut
        }
    }
    
    /**
     * Parse un timestamp Elite Dangerous
     */
    private LocalDateTime parseTimestamp(String timestamp) {
        try {
            // Format: 2025-09-19T12:12:54Z
            return LocalDateTime.parse(timestamp.replace("Z", ""), 
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
    
    /**
     * Génère un résumé des missions
     */
    public String generateMissionSummary() {
        List<Mission> missions = getMissionsFromLastWeek();
        
        long activeCount = missions.stream().filter(m -> m.getStatus() == MissionStatus.ACTIVE).count();
        long completedCount = missions.stream().filter(m -> m.getStatus() == MissionStatus.COMPLETED).count();
        long failedCount = missions.stream().filter(m -> m.getStatus() == MissionStatus.FAILED).count();
        
        long totalReward = missions.stream()
            .filter(m -> m.getStatus() == MissionStatus.COMPLETED)
            .mapToLong(Mission::getReward)
            .sum();
        
        StringBuilder summary = new StringBuilder();
        summary.append("=== RÉSUMÉ DES MISSIONS (7 DERNIERS JOURS) ===\n");
        summary.append(String.format("Missions actives: %d\n", activeCount));
        summary.append(String.format("Missions complétées: %d\n", completedCount));
        summary.append(String.format("Missions abandonnées: %d\n", failedCount));
        summary.append(String.format("Total des récompenses: %,d Cr\n", totalReward));
        summary.append("\n=== MISSIONS ACTIVES ===\n");
        
        missions.stream()
            .filter(m -> m.getStatus() == MissionStatus.ACTIVE)
            .sorted((m1, m2) -> m1.getFaction().compareTo(m2.getFaction()))
            .forEach(mission -> {
                String targetInfo = mission.getTargetFaction() != null ? mission.getTargetFaction() : "Pirates";
        if (mission.getTargetSystem() != null) {
            targetInfo += " - " + mission.getTargetSystem();
        }
        
        String killsText;
        if (mission.getStatus() == MissionStatus.COMPLETED) {
            // Pour les missions complétées, afficher y/y
            int targetCount = mission.getTargetCount();
            killsText = String.format("%d/%d", targetCount, targetCount);
        } else {
            // Pour les missions actives, afficher x/y
            int currentCount = mission.getCurrentCount();
            int targetCount = mission.getTargetCount();
            killsText = String.format("%d/%d", currentCount, targetCount);
        }
        
        // Informations temporelles
        String timeInfo = "";
        if (mission.getAcceptedTime() != null) {
            timeInfo = " - Accepté: " + mission.getAcceptedTime().format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));
            if (mission.getExpiry() != null) {
                long hoursRemaining = java.time.Duration.between(LocalDateTime.now(), mission.getExpiry()).toHours();
                if (hoursRemaining > 0) {
                    timeInfo += " (Restant: " + hoursRemaining + "h)";
                } else {
                    timeInfo += " (Expirée)";
                }
            }
        }
        
        summary.append(String.format("- %s (%s): %s kills - %,d Cr%s\n",
                    mission.getName(),
                    mission.getFaction(),
                    killsText,
                    mission.getReward(),
                    timeInfo));
            });
        
        return summary.toString();
    }
}
