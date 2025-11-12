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
import java.util.List;

public class BioSpeciesService {

    private static final BioSpeciesService INSTANCE = new BioSpeciesService();

    public static BioSpeciesService getInstance() {
        return INSTANCE;
    }
    private BioSpeciesService() {}

    public List<BioSpecies> getSpecies() throws URISyntaxException, IOException {
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
            String resourceName =  filePath.getFileName().toString();

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
        return allSpecies;
    }
}
