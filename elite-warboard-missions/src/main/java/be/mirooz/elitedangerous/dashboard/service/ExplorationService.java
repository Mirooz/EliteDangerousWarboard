package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.biologic.BioSpecies;
import be.mirooz.elitedangerous.dashboard.model.exploration.ACelesteBody;
import be.mirooz.elitedangerous.dashboard.model.exploration.PlaneteDetail;
import be.mirooz.elitedangerous.dashboard.model.exploration.Position;
import be.mirooz.elitedangerous.dashboard.model.exploration.SystemVisited;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.SystemVisitedRegistry;
import lombok.Getter;

import java.util.*;

public class ExplorationService {
    private static final ExplorationService INSTANCE = new ExplorationService();

    private final SystemVisitedRegistry systemVisitedRegistry = SystemVisitedRegistry.getInstance();

    @Getter
    // Analyse biologique en cours
    private PlaneteDetail currentAnalysisPlanet = null;
    private BioSpecies currentAnalysisSpecies = null;

    private ExplorationService() {}
    public static ExplorationService getInstance() {
        return INSTANCE;
    }

    /**
     * Ajoute ou met à jour un système visité.
     */
    public void addOrUpdateSystem(String currentSystem, Collection<ACelesteBody> planets, String timestamp) {
        ;
        SystemVisited previous = systemVisitedRegistry.getSystems().get(currentSystem);

        SystemVisited system = SystemVisited.builder()
                .systemName(currentSystem)
                .numBodies(planets.size())
                .build();

        // Définition du premier body (utile pour timestamp & firstDiscover)
        planets.stream().findFirst().ifPresentOrElse(
                p -> {
                    system.setFirstDiscover(!p.isWasDiscovered());
                    system.setFirstVisitedTime(p.getTimestamp());
                    system.setLastVisitedTime(p.getTimestamp());
                },
                () -> {
                    system.setFirstVisitedTime(timestamp);
                    system.setLastVisitedTime(timestamp);
                }
        );


        // Mise à jour si déjà visité
        if (previous != null) {
            system.setFirstDiscover(previous.isFirstDiscover());
            system.setFirstVisitedTime(previous.getFirstVisitedTime());
            system.setLastVisitedTime(timestamp);
            system.setNumberVisited(previous.getNumberVisited() + 1);
        }

        // Copie triée des planètes
        system.setCelesteBodies(new ArrayList<>(planets));

        // Stockage
        systemVisitedRegistry.getSystems().put(currentSystem, system);
    }
    public void setCurrentBiologicalAnalysis(PlaneteDetail planeteDetail, BioSpecies species) {
        if (planeteDetail != null &&  species != null) {
            if (planeteDetail != currentAnalysisPlanet || species != currentAnalysisSpecies) {
                species.removeAllSamples();
                clearCurrentBiologicalAnalysis();
            }
            Position samplePosition = DirectionReaderService.getInstance().readCurrentPosition(planeteDetail.getRadius());
            DirectionReaderService.getInstance().getCurrentBiologicalSamplePositions().add(samplePosition);
            DirectionReaderService.getInstance().startWatchingStatusFile(planeteDetail.getRadius());

        }
        this.currentAnalysisPlanet = planeteDetail;
        this.currentAnalysisSpecies = species;
    }

    public List<Position> getCurrentBiologicalSamplesPosition() {
        if (DirectionReaderService.getInstance().getCurrentBiologicalSamplePositions().isEmpty()) {
            return null;
        }
        return DirectionReaderService.getInstance().getCurrentBiologicalSamplePositions();
    }
    public Position getCurrentPosition(){
        return DirectionReaderService.getInstance().getCurrentPosition();
    }
    /**
     * Réinitialise l'analyse biologique en cours (quand l'analyse est terminée).
     */
    public void clearCurrentBiologicalAnalysis() {
        DirectionReaderService.getInstance().stopWatchingStatusFile();
        this.currentAnalysisPlanet = null;
        this.currentAnalysisSpecies = null;
        DirectionReaderService.getInstance().getCurrentBiologicalSamplePositions().clear();
    }

    /**
     * Indique si une analyse biologique est en cours.
     */
    public boolean isBiologicalAnalysisInProgress() {
        return currentAnalysisPlanet != null && currentAnalysisSpecies != null;
    }

    public boolean isCurrentBiologicalAnalysisOnCurrentPlanet(String planeteName) {
        return currentAnalysisPlanet != null && currentAnalysisPlanet.getBodyName().equals(planeteName);
    }
}
