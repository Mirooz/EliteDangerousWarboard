package be.mirooz.elitedangerous.dashboard.model.registries.exploration;

import be.mirooz.elitedangerous.dashboard.controller.ui.context.DashboardContext;
import be.mirooz.elitedangerous.dashboard.model.exploration.ACelesteBody;
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

    private final ObservableMap<Integer, ACelesteBody> planetesMap =
            FXCollections.observableHashMap();

    private PlaneteRegistry() {
    }
    private String currentStarSystem;

    public static PlaneteRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Ajoute ou met à jour une planète dans le registre.
     * Utilise le bodyID comme clé unique.
     */
    public void addOrUpdateBody(ACelesteBody body) {
        ACelesteBody existing = planetesMap.get(body.getBodyID());

        if (existing instanceof PlaneteDetail oldP
                && body instanceof PlaneteDetail newP) {
            // Au lieu de remplacer l'objet, on met juste à jour les champs
            oldP.updateFrom(newP);
            return;
        }

        planetesMap.put(body.getBodyID(), body);
    }


    /**
     * Récupère une planète par son bodyID.
     */
    public Optional<ACelesteBody> getByBodyID(int bodyID) {
        return Optional.ofNullable(planetesMap.get(bodyID));
    }

    /**
     * Récupère une planète par son nom et système stellaire.
     */
    public Optional<ACelesteBody> getPlaneteByName(String bodyName, String starSystem) {
        return planetesMap.values().stream()
                .filter(p -> p.getBodyName().equals(bodyName) && p.getStarSystem().equals(starSystem))
                .findFirst();
    }

    /**
     * Récupère toutes les planètes d'un système stellaire.
     */
    public java.util.List<ACelesteBody> getPlanetesBySystem(String starSystem) {
        return planetesMap.values().stream()
                .filter(p -> p.getStarSystem().equals(starSystem))
                .toList();
    }

    /**
     * Ajoute un listener pour les changements du registre.
     */
    public void addPlaneteMapListener(Runnable action) {
        planetesMap.addListener((MapChangeListener<Integer, ACelesteBody>) change -> {
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
        currentStarSystem = null;
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
    public Collection<ACelesteBody> getAllPlanetes() {
        return planetesMap.values();
    }

    public void setAllPlanetes(Collection<ACelesteBody> planetes) {
        clear();
        planetes.forEach( planete ->
                planetesMap.put(planete.getBodyID(), planete)
        );
    }
}

