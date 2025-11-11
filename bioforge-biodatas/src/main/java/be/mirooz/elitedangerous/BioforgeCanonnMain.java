package be.mirooz.elitedangerous;

import be.mirooz.elitedangerous.species.biologic.utils.BioSpeciesFactory;
import be.mirooz.elitedangerous.species.biologic.utils.HistogramBioSpecies;
import be.mirooz.elitedangerous.species.biologic.utils.VariantMethods;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class BioforgeCanonnMain {
    public static void main(String[] args) throws IOException, URISyntaxException {
        List<HistogramBioSpecies> allSpecies = new ArrayList<>();

        getSpecies(allSpecies);
        System.out.println("✅ Total species loaded: " + allSpecies.size());
        allSpecies.forEach(
                e -> {
                    System.out.println(e.getFullName() + " " + e.getVariantMethod() + " " + e.getColorConditionName());
                }
        );

    }

    private static void getSpecies(List<HistogramBioSpecies> allSpecies) throws URISyntaxException, IOException {
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
                List<HistogramBioSpecies> telaVariants = BioSpeciesFactory.createFromJsonResource(
                        inputStream, Double.parseDouble(resourceName.split("_")[1].split("m")[0])
                );

                allSpecies.addAll(telaVariants);
            }
        }
    }
}
