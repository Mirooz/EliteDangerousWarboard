package be.mirooz.elitedangerous.dashboard.service.persistence;

import be.mirooz.elitedangerous.dashboard.persistence.CarrierStatusStore;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PersistenceService {

    private static final PersistenceService INSTANCE = new PersistenceService();

    public static PersistenceService getInstance() {
        return INSTANCE;
    }

    private final CarrierStatusStore carrierStatusStore;
    private final Path persistenceFile;

    private PersistenceService() {

        this.persistenceFile = Paths.get(
                System.getProperty("user.home"),
                ".elite-warboard",
                "carrier-status.json"
        );

        this.carrierStatusStore = new CarrierStatusStore(persistenceFile);
    }

    // -------- API --------

    public void load() {
        try {
            boolean loaded = carrierStatusStore.loadIfExists();
            if (loaded) {
                System.out.println("CarrierStatus chargé depuis " + persistenceFile);
            } else {
                System.out.println("Aucun fichier trouvé : " + persistenceFile);
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement persistence");
            e.printStackTrace();
        }
    }

    public void save() {
        try {
            carrierStatusStore.save();
            System.out.println("CarrierStatus sauvegardé dans " + persistenceFile);
        } catch (Exception e) {
            System.err.println("Erreur sauvegarde persistence");
            e.printStackTrace();
        }
    }

    public void delete() {
        try {
            carrierStatusStore.deleteIfExists();
            System.out.println("Fichier supprimé : " + persistenceFile);
        } catch (Exception e) {
            System.err.println("Erreur suppression persistence");
            e.printStackTrace();
        }
    }
}