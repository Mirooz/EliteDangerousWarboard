package be.mirooz.elitedangerous.dashboard.model.registries.colonisation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Synchronized;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Historique des amarrages sur des stations / sites de colonisation (sans limite de taille).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ColonisationDocksRegistry {

    private static final ColonisationDocksRegistry INSTANCE = new ColonisationDocksRegistry();

    private final List<ColonisationDockEntry> docks = new ArrayList<>();

    public static ColonisationDocksRegistry getInstance() {
        return INSTANCE;
    }

    @Synchronized
    public void record(ColonisationDockEntry entry) {
        if (entry == null) {
            return;
        }
        docks.add(entry);
    }

    @Synchronized
    public List<ColonisationDockEntry> getDocks() {
        return Collections.unmodifiableList(new ArrayList<>(docks));
    }

    /**
     * Réinitialise l'historique (tests ou rechargement de session).
     */
    @Synchronized
    public void clear() {
        docks.clear();
    }
}
