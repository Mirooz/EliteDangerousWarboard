package be.mirooz.elitedangerous.service;

import be.mirooz.elitedangerous.BioforgeCanonnMain;
import be.mirooz.elitedangerous.biologic.BioSpecies;
import be.mirooz.elitedangerous.biologic.BioSpeciesFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
    private List<BioSpecies> loadSpecies() throws URISyntaxException, IOException {
        List<BioSpecies> allSpecies = new ArrayList<>();

        // 1️⃣ ClassLoader pour accéder aux ressources du classpath
        ClassLoader classLoader = BioforgeCanonnMain.class.getClassLoader();

        // 2️⃣ Récupération du dossier "bacterium" dans le classpath
        URL resourceUrl = classLoader.getResource(".");
        if (resourceUrl == null) {
            throw new FileNotFoundException("Le dossier est introuvable dans le classpath.");
        }

        // 3️⃣ Lister tous les fichiers JSON du dossier
        Path folderPath = Paths.get(resourceUrl.toURI());
        List<Path> jsonFiles = Files.list(folderPath)
                .filter(p -> p.toString().endsWith(".json"))
                .toList();

        // 4️⃣ Charger chaque ressource correctement
        for (Path filePath : jsonFiles) {
            String resourceName = filePath.getFileName().toString();

            try (InputStream inputStream = classLoader.getResourceAsStream(resourceName)) {
                if (inputStream == null) {
                    System.err.println("❌ Resource not found in classpath: " + resourceName);
                    continue;
                }

                // ✅ Appel correct si ta méthode prend un InputStream
                List<BioSpecies> telaVariants = BioSpeciesFactory.createFromJsonResource(
                        inputStream, Double.parseDouble(resourceName.split("_")[1].split("m")[0])
                );

                allSpecies.addAll(telaVariants);
            }
        }
        
        // Retourner une liste non modifiable pour éviter les modifications accidentelles
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
