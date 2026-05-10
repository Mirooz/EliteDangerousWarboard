package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.biologic.BioSpecies;
import be.mirooz.elitedangerous.dashboard.model.exploration.ACelesteBody;
import be.mirooz.elitedangerous.dashboard.model.exploration.PlaneteDetail;
import be.mirooz.elitedangerous.dashboard.model.exploration.Position;
import be.mirooz.elitedangerous.dashboard.model.exploration.SystemVisited;
import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.ExplorationDataSaleRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.PlaneteRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.SystemVisitedRegistry;
import be.mirooz.elitedangerous.dashboard.view.common.context.DashboardContext;

import java.util.*;

public class ExplorationService {
    private static final ExplorationService INSTANCE = new ExplorationService();

    private final SystemVisitedRegistry systemVisitedRegistry = SystemVisitedRegistry.getInstance();
    private final ExplorationDataSaleRegistry explorationDataSaleRegistry = ExplorationDataSaleRegistry.getInstance();
    private final PlaneteRegistry planeteRegistry = PlaneteRegistry.getInstance();

    private ExplorationService() {
    }

    private void setAnalysisInRegistry(String bodyName, String speciesId) {
        explorationDataSaleRegistry.setCurrentAnalysisBodyName(bodyName);
        explorationDataSaleRegistry.setCurrentAnalysisSpeciesId(speciesId);
    }

    public String getCurrentAnalysisBodyName() {
        return explorationDataSaleRegistry.getCurrentAnalysisBodyName();
    }

    public String getCurrentAnalysisSpeciesId() {
        return explorationDataSaleRegistry.getCurrentAnalysisSpeciesId();
    }

    /**
     * Retire les échantillons / logs de l'espèce confirmée identifiée sur le corps indiqué
     * (même logique qu'avant avec la référence {@link BioSpecies}).
     */
    private void removeSamplesFromConfirmedSpecies(String bodyName, String speciesId) {
        if (bodyName == null || speciesId == null) {
            return;
        }
        String starSystem = CommanderStatus.getInstance().getCurrentStarSystem();
        if (starSystem == null || starSystem.isBlank()) {
            return;
        }
        planeteRegistry.getPlaneteByName(bodyName, starSystem)
                .filter(PlaneteDetail.class::isInstance)
                .map(PlaneteDetail.class::cast)
                .flatMap(p -> p.getConfirmedSpecies().stream()
                        .filter(s -> speciesId.equalsIgnoreCase(s.getId()))
                        .findFirst())
                .ifPresent(BioSpecies::removeAllSamples);
    }

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
        system.setCelesteBodies(planets);

        // Stockage
        systemVisitedRegistry.getSystems().put(currentSystem, system);
        explorationDataSaleRegistry.resyncSystemVisitedWithRegistry(currentSystem);
    }
    public void setCurrentBiologicalAnalysis(PlaneteDetail planeteDetail, BioSpecies species) {
        String prevBody = explorationDataSaleRegistry.getCurrentAnalysisBodyName();
        String prevSpeciesId = explorationDataSaleRegistry.getCurrentAnalysisSpeciesId();

        if (planeteDetail != null && species != null) {
            boolean bodyChanged = !Objects.equals(planeteDetail.getBodyName(), prevBody);
            boolean speciesChanged = !Objects.equals(species.getId(), prevSpeciesId);
            if (bodyChanged || speciesChanged) {
                if (prevSpeciesId != null) {
                    removeSamplesFromConfirmedSpecies(prevBody, prevSpeciesId);
                }
                clearCurrentBiologicalAnalysis();
            }
            if (!DashboardContext.getInstance().isBatchLoading()) {
                Position samplePosition = DirectionReaderService.getInstance().readCurrentPosition(planeteDetail.getRadius());
                if (samplePosition != null) {
                    DirectionReaderService.getInstance().getCurrentBiologicalSamplePositions().add(samplePosition);
                }
                DirectionReaderService.getInstance().startWatchingStatusFile(planeteDetail.getRadius(), species.getColonyRangeMeters());
            }

        }
        if (planeteDetail != null && species != null) {
            setAnalysisInRegistry(planeteDetail.getBodyName(), species.getId());
        } else {
            setAnalysisInRegistry(null, null);
        }
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
        setAnalysisInRegistry(null, null);
        DirectionReaderService.getInstance().getCurrentBiologicalSamplePositions().clear();
    }

    /**
     * Indique si une analyse biologique est en cours.
     */
    public boolean isBiologicalAnalysisInProgress() {
        return explorationDataSaleRegistry.getCurrentAnalysisBodyName() != null
                && explorationDataSaleRegistry.getCurrentAnalysisSpeciesId() != null;
    }

    public boolean isCurrentBiologicalAnalysisOnCurrentPlanet(String planeteName) {
        String body = explorationDataSaleRegistry.getCurrentAnalysisBodyName();
        return body != null && body.equals(planeteName);
    }
}
