package be.mirooz.elitedangerous.service;

import be.mirooz.elitedangerous.BioforgeCanonnMain;
import be.mirooz.elitedangerous.biologic.BioSpecies;
import be.mirooz.elitedangerous.biologic.BioSpeciesFactory;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service pour charger et gérer les espèces biologiques.
 * Le calcul des espèces n'est effectué qu'une seule fois et mis en cache.
 */
public class BioSpeciesService {

    private static final BioSpeciesService INSTANCE = new BioSpeciesService();

    private volatile List<BioSpecies> cachedSpecies = null;
    private final Object loadLock = new Object();

    public static BioSpeciesService getInstance() {
        return INSTANCE;
    }

    private BioSpeciesService() {}

    /**
     * Récupère la liste des espèces biologiques.
     * Le calcul n'est effectué qu'une seule fois, les résultats suivants sont servis depuis le cache.
     *
     * @return La liste des espèces biologiques (non modifiable)
     * @throws URISyntaxException Si une erreur survient lors de l'accès aux ressources
     * @throws IOException Si une erreur survient lors de la lecture des fichiers
     */
    public List<BioSpecies> getSpecies() throws URISyntaxException, IOException {
        // Double-check locking pattern pour la performance
        if (cachedSpecies == null) {
            synchronized (loadLock) {
                if (cachedSpecies == null) {
                    cachedSpecies = loadSpecies();
                }
            }
        }
        return cachedSpecies;
    }

    /**
     * Charge les espèces biologiques depuis les fichiers JSON.
     * Cette méthode n'est appelée qu'une seule fois.
     */
    private List<BioSpecies> loadSpecies() throws IOException {
        List<BioSpecies> allSpecies = new ArrayList<>();

        ClassLoader cl = BioforgeCanonnMain.class.getClassLoader();

        // Charger le fichier index
        try (InputStream indexStream = cl.getResourceAsStream("species/index.txt")) {
            if (indexStream == null) {
                throw new FileNotFoundException("species/index.txt introuvable dans le JAR !");
            }

            List<String> fileNames = new BufferedReader(new InputStreamReader(indexStream))
                    .lines()
                    .filter(l -> !l.isBlank())
                    .toList();

            for (String fileName : fileNames) {
                try (InputStream jsonStream = cl.getResourceAsStream("species/" + fileName)) {
                    if (jsonStream == null) {
                        System.err.println("❌ JSON introuvable : species/" + fileName);
                        continue;
                    }

                    double distance = Double.parseDouble(fileName.split("_")[1].replace("m.json", ""));
                    List<BioSpecies> variants = BioSpeciesFactory.createFromJsonResource(jsonStream, distance);
                    allSpecies.addAll(variants);
                }
            }
        }

        return Collections.unmodifiableList(allSpecies);
    }

    /**
     * Réinitialise le cache (utile pour les tests ou le rechargement).
     */
    public void clearCache() {
        synchronized (loadLock) {
            cachedSpecies = null;
        }
    }
}
