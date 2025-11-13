package be.mirooz.elitedangerous.dashboard.model.registries;

import be.mirooz.elitedangerous.dashboard.controller.ui.context.DashboardContext;
import be.mirooz.elitedangerous.dashboard.model.exploration.BiologicalSignalProcessor;
import be.mirooz.elitedangerous.dashboard.model.exploration.PendingBiologicalSignal;
import be.mirooz.elitedangerous.dashboard.model.exploration.PlaneteDetail;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import lombok.Data;

import java.util.Optional;

/**
 * Singleton pour stocker les détails des planètes scannées, observable par la UI.
 * Utilise le bodyID comme clé unique.
 */
@Data
public class PlaneteRegistry {

    private static final PlaneteRegistry INSTANCE = new PlaneteRegistry();

    private final ObservableMap<Integer, PlaneteDetail> planetesMap =
            FXCollections.observableHashMap();

    private PlaneteRegistry() {
    }

    public static PlaneteRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Ajoute ou met à jour une planète dans le registre.
     * Utilise le bodyID comme clé unique.
     */
    public void addOrUpdatePlanete(PlaneteDetail planete) {
        if (planete != null && planete.getBodyID() > 0) {
            planetesMap.put(planete.getBodyID(), planete);
        }
    }

    /**
     * Récupère une planète par son bodyID.
     */
    public Optional<PlaneteDetail> getPlaneteByBodyID(int bodyID) {
        return Optional.ofNullable(planetesMap.get(bodyID));
    }

    /**
     * Récupère une planète par son nom et système stellaire.
     */
    public Optional<PlaneteDetail> getPlaneteByName(String bodyName, String starSystem) {
        return planetesMap.values().stream()
                .filter(p -> p.getBodyName().equals(bodyName) && p.getStarSystem().equals(starSystem))
                .findFirst();
    }

    /**
     * Récupère toutes les planètes d'un système stellaire.
     */
    public java.util.List<PlaneteDetail> getPlanetesBySystem(String starSystem) {
        return planetesMap.values().stream()
                .filter(p -> p.getStarSystem().equals(starSystem))
                .toList();
    }

    /**
     * Ajoute un listener pour les changements du registre.
     */
    public void addPlaneteMapListener(Runnable action) {
        planetesMap.addListener((MapChangeListener<Integer, PlaneteDetail>) change -> {
            if (!DashboardContext.getInstance().isBatchLoading()) {
                Platform.runLater(action);
            }
        });
    }

    /**
     * Vide le registre.
     */
    public void clear() {
        planetesMap.clear();
        BiologicalSignalProcessor.getInstance().clear();
    }

    /**
     * Retourne le nombre de planètes dans le registre.
     */
    public int size() {
        return planetesMap.size();
    }

    /**
     * Retourne toutes les planètes.
     */
    public java.util.Collection<PlaneteDetail> getAllPlanetes() {
        return planetesMap.values();
    }
}

