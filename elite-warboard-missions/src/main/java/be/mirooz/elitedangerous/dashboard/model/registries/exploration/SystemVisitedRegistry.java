package be.mirooz.elitedangerous.dashboard.model.registries.exploration;

import be.mirooz.elitedangerous.dashboard.model.exploration.SystemVisited;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import lombok.Data;

import java.util.Collection;

/**
 * Registry pour stocker les systèmes visités.
 * Singleton observable pour la UI.
 * Utilise le nom du système comme clé.
 */
@Data
public class SystemVisitedRegistry {

    private static final SystemVisitedRegistry INSTANCE = new SystemVisitedRegistry();

    private final ObservableMap<String, SystemVisited> systems = FXCollections.observableHashMap();

    private SystemVisitedRegistry() {
    }

    public static SystemVisitedRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Ajoute ou met à jour un système visité.
     * Marque le système comme vendu (sold = true).
     */
    public void addOrUpdateSystem(String systemName, int numBodies,boolean sold) {
        SystemVisited system = SystemVisited.builder()
                .systemName(systemName)
                .numBodies(numBodies)
                .sold(sold)
                .build();
        systems.put(systemName, system);
    }

    /**
     * Récupère un système par son nom.
     */
    public SystemVisited getSystem(String systemName) {
        return systems.get(systemName);
    }

    /**
     * Récupère tous les systèmes.
     */
    public Collection<SystemVisited> getAllSystems() {
        return systems.values();
    }

    /**
     * Vide le registry.
     */
    public void clear() {
        systems.clear();
    }

    /**
     * Retourne le nombre de systèmes dans le registry.
     */
    public int size() {
        return systems.size();
    }
}

