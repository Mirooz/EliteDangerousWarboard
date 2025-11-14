package be.mirooz.elitedangerous.dashboard.model.registries.exploration;

import be.mirooz.elitedangerous.dashboard.model.exploration.ACelesteBody;
import be.mirooz.elitedangerous.dashboard.model.exploration.PlaneteDetail;
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
    public void addOrUpdateSystem(String currentSystem, Collection<ACelesteBody> planets, String timestamp) {
;
        SystemVisited previous = systems.get(currentSystem);

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
        systems.put(currentSystem, system);
    }

    private static List<ACelesteBody> getCelesteBodiesClone(PlaneteRegistry pr) {
        List<ACelesteBody> sortedBodies =
                new ArrayList<>(pr.getAllPlanetes());
        sortedBodies.sort(Comparator.comparing(ACelesteBody::getBodyID));
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