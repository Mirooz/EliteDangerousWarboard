package be.mirooz.elitedangerous.dashboard.service.webservice.eddn;

import be.mirooz.elitedangerous.dashboard.service.PreferencesService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Lit les fichiers compagnons du jeu déposés dans le dossier du journal :
 * {@code Market.json}, {@code Shipyard.json}, {@code Outfitting.json}, {@code NavRoute.json}.
 *
 * <p>Ces fichiers sont écrits par le jeu en même temps que l'event journal correspondant.
 * Pour EDDN, ils contiennent la donnée complète (ex. liste des commodities du marché),
 * absente des events journal eux-mêmes.
 */
public final class EddnJournalFileReader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private EddnJournalFileReader() {}

    public static JsonNode readMarket() {
        return readJsonFile("Market.json");
    }

    public static JsonNode readShipyard() {
        return readJsonFile("Shipyard.json");
    }

    public static JsonNode readOutfitting() {
        return readJsonFile("Outfitting.json");
    }

    public static JsonNode readNavRoute() {
        return readJsonFile("NavRoute.json");
    }

    private static JsonNode readJsonFile(String fileName) {
        try {
            String folder = PreferencesService.getInstance().getJournalFolder();
            if (folder == null || folder.isBlank()) {
                return null;
            }
            Path path = Paths.get(folder, fileName);
            if (!Files.exists(path)) {
                return null;
            }
            byte[] content = Files.readAllBytes(path);
            if (content.length == 0) {
                return null;
            }
            return MAPPER.readTree(content);
        } catch (IOException e) {
            System.err.println("EDDN: lecture " + fileName + " : " + e.getMessage());
            return null;
        }
    }
}
