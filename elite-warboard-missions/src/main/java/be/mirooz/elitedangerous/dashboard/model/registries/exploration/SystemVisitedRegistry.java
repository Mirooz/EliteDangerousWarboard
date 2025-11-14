package be.mirooz.elitedangerous.dashboard.model.registries.exploration;

import be.mirooz.elitedangerous.dashboard.model.exploration.AbstractCelesteBody;
import be.mirooz.elitedangerous.dashboard.model.exploration.SystemVisited;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.transformation.SortedList;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Registry pour stocker les systèmes visités.
 * - Singleton observable pour la UI
 * - Lookup O(1) par nom grâce à systems (map)
 * - Liste triée automatiquement par lastVisitedTime pour l'affichage UI
 */
@Data
public class SystemVisitedRegistry {

    private static final SystemVisitedRegistry INSTANCE = new SystemVisitedRegistry();

    /** Lookup rapide par nom du système */
    private final ObservableMap<String, SystemVisited> systems =
            FXCollections.observableHashMap();

    /** Liste reflétant automatiquement la map */
    private final ObservableList<SystemVisited> systemsList =
            FXCollections.observableArrayList();

    /** Liste triée automatiquement par lastVisitedTime (desc) */
    private final SortedList<SystemVisited> sortedSystems =
            new SortedList<>(systemsList, Comparator
                    .comparing(SystemVisited::getLastVisitedTime)
                    .reversed()
            );

    private SystemVisitedRegistry() {
        // Synchronise automatiquement la map -> liste
        systems.addListener((MapChangeListener<String, SystemVisited>) change -> {
            if (change.wasRemoved()) {
                systemsList.remove(change.getValueRemoved());
            }
            if (change.wasAdded()) {
                systemsList.add(change.getValueAdded());
            }
        });
    }

    public static SystemVisitedRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Ajoute ou met à jour un système visité.
     */
    public void addOrUpdateSystem(PlaneteRegistry planets, String timestamp) {

        String systemName = planets.getCurrentStarSystem();
        SystemVisited previous = systems.get(systemName);

        SystemVisited system = SystemVisited.builder()
                .systemName(systemName)
                .numBodies(planets.getAllPlanetes().size())
                .build();

        // Définition du premier body (utile pour timestamp & firstDiscover)
        planets.getAllPlanetes().stream().findFirst().ifPresent(p -> {
            system.setFirstDiscover(!p.isWasDiscovered());
            system.setFirstVisitedTime(p.getTimestamp());
            system.setLastVisitedTime(p.getTimestamp());
        });

        // Mise à jour si déjà visité
        if (previous != null) {
            system.setFirstDiscover(previous.isFirstDiscover());
            system.setFirstVisitedTime(previous.getFirstVisitedTime());
            system.setLastVisitedTime(timestamp);
            system.setNumberVisited(previous.getNumberVisited() + 1);
        }

        // Copie triée des planètes
        system.setCelesteBodies(getCelesteBodiesClone(planets));

        // Stockage
        systems.put(systemName, system);
    }

    private static List<AbstractCelesteBody> getCelesteBodiesClone(PlaneteRegistry pr) {
        List<AbstractCelesteBody> sortedBodies =
                new ArrayList<>(pr.getAllPlanetes());
        sortedBodies.sort(Comparator.comparing(AbstractCelesteBody::getBodyID));
        return sortedBodies;
    }

    public void setSold(String systemName, int numBodies) {
        SystemVisited s = systems.get(systemName);
        if (s != null) {
            s.setSold(true);
        }
    }

    public SystemVisited getSystem(String systemName) {
        return systems.get(systemName);
    }

    public Collection<SystemVisited> getAllSystems() {
        return systems.values();
    }

    /** Liste triée automatiquement pour la UI */
    public SortedList<SystemVisited> getSortedSystems() {
        return sortedSystems;
    }

    public void clear() {
        systems.clear();
    }

    public int size() {
        return systems.size();
    }
}



/*
package be.mirooz.elitedangerous.dashboard.model.registries.exploration;

import be.mirooz.elitedangerous.dashboard.model.exploration.AbstractCelesteBody;
import be.mirooz.elitedangerous.dashboard.model.exploration.SystemVisited;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

*/
/**
 * Registry pour stocker les systèmes visités.
 * Singleton observable pour la UI.
 * Utilise le nom du système comme clé.
 *//*

@Data
public class SystemVisitedRegistry {

    private static final SystemVisitedRegistry INSTANCE = new SystemVisitedRegistry();

    private final ObservableMap<String, SystemVisited> systems = FXCollections.observableHashMap();

    private SystemVisitedRegistry() {
    }

    public static SystemVisitedRegistry getInstance() {
        return INSTANCE;
    }

    */
/**
     * Ajoute ou met à jour un système visité.
     * Marque le système comme vendu (sold = true).
     *//*

    public void addOrUpdateSystem(PlaneteRegistry planeteRegistry,String timestamp) {
        SystemVisited system = SystemVisited.builder()
                .systemName(planeteRegistry.getCurrentStarSystem())
                .numBodies(planeteRegistry.getAllPlanetes().size())
                .build();
        planeteRegistry.getAllPlanetes().stream().findFirst().ifPresent(p -> {
            system.setFirstDiscover(!p.isWasDiscovered());
            system.setFirstVisitedTime(p.getTimestamp());
            system.setLastVisitedTime(p.getTimestamp());
        });
        if (systems.containsKey(planeteRegistry.getCurrentStarSystem())){
            system.setFirstDiscover(systems.get(planeteRegistry.getCurrentStarSystem()).isFirstDiscover());
            system.setFirstVisitedTime(systems.get(planeteRegistry.getCurrentStarSystem()).getFirstVisitedTime());
            system.setLastVisitedTime(timestamp);
            system.setNumberVisited(systems.get(planeteRegistry.getCurrentStarSystem()).getNumberVisited() + 1);
        }
        List<AbstractCelesteBody> sortedBodies = getCelesteBodiesClone(planeteRegistry);
        system.setCelesteBodies(sortedBodies);
        systems.put(planeteRegistry.getCurrentStarSystem(), system);
    }

    private static List<AbstractCelesteBody> getCelesteBodiesClone(PlaneteRegistry planeteRegistry) {
        List<AbstractCelesteBody> sortedBodies = new ArrayList<>(planeteRegistry.getAllPlanetes());
        // Trier par bodyId (en supposant getBodyId())
        sortedBodies.sort(Comparator.comparing(AbstractCelesteBody::getBodyID));
        return sortedBodies;
    }

    public void setSold(String systemName, int numBodies) {
        if (systems.containsKey(systemName)) {
            systems.get(systemName).setNumBodies(numBodies);
            systems.get(systemName).setSold(true);
        }
    }

    */
/**
     * Récupère un système par son nom.
     *//*

    public SystemVisited getSystem(String systemName) {
        return systems.get(systemName);
    }

    */
/**
     * Récupère tous les systèmes.
     *//*

    public Collection<SystemVisited> getAllSystems() {
        return systems.values();
    }

    */
/**
     * Vide le registry.
     *//*

    public void clear() {
        systems.clear();
    }

    */
/**
     * Retourne le nombre de systèmes dans le registry.
     *//*

    public int size() {
        return systems.size();
    }
}

*/
