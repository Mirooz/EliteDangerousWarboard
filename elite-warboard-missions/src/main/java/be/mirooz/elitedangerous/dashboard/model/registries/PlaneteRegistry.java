package be.mirooz.elitedangerous.dashboard.model.registries;

import be.mirooz.elitedangerous.dashboard.controller.ui.context.DashboardContext;
import be.mirooz.elitedangerous.dashboard.model.exploration.AbstractCelesteBody;
import be.mirooz.elitedangerous.dashboard.model.exploration.BiologicalSignalProcessor;
import be.mirooz.elitedangerous.dashboard.model.exploration.PlaneteDetail;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import lombok.Data;

import java.util.Collection;
import java.util.Optional;

/**
 * Singleton pour stocker les détails des planètes scannées, observable par la UI.
 * Utilise le bodyID comme clé unique.
 */
@Data
public class PlaneteRegistry {

    private static final PlaneteRegistry INSTANCE = new PlaneteRegistry();

    private final ObservableMap<Integer, AbstractCelesteBody> planetesMap =
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
    public void addOrUpdateBody(AbstractCelesteBody body) {
        if (body == null) return;
        AbstractCelesteBody existing = planetesMap.get(body.getBodyID());
        if (existing != null) {
            // On préserve les flags déjà connus
            body.setWasFootfalled(body.isWasFootfalled() || existing.isWasFootfalled());
            body.setWasMapped(body.isWasMapped() || existing.isWasMapped());
            body.setWasDiscovered(body.isWasDiscovered() || existing.isWasDiscovered());
        }
        planetesMap.put(body.getBodyID(), body);
    }


    /**
     * Récupère une planète par son bodyID.
     */
    public Optional<AbstractCelesteBody> getByBodyID(int bodyID) {
        return Optional.ofNullable(planetesMap.get(bodyID));
    }

    /**
     * Récupère une planète par son nom et système stellaire.
     */
    public Optional<AbstractCelesteBody> getPlaneteByName(String bodyName, String starSystem) {
        return planetesMap.values().stream()
                .filter(p -> p.getBodyName().equals(bodyName) && p.getStarSystem().equals(starSystem))
                .findFirst();
    }

    /**
     * Récupère toutes les planètes d'un système stellaire.
     */
    public java.util.List<AbstractCelesteBody> getPlanetesBySystem(String starSystem) {
        return planetesMap.values().stream()
                .filter(p -> p.getStarSystem().equals(starSystem))
                .toList();
    }

    /**
     * Ajoute un listener pour les changements du registre.
     */
    public void addPlaneteMapListener(Runnable action) {
        planetesMap.addListener((MapChangeListener<Integer, AbstractCelesteBody>) change -> {
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
    public Collection<AbstractCelesteBody> getAllPlanetes() {
        return planetesMap.values();
    }
}

