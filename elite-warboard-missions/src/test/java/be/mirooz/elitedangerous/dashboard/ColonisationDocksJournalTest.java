package be.mirooz.elitedangerous.dashboard;

import be.mirooz.elitedangerous.dashboard.model.registries.colonisation.ColonisationDockEntry;
import be.mirooz.elitedangerous.dashboard.model.registries.colonisation.ColonisationDocksRegistry;
import be.mirooz.elitedangerous.dashboard.service.ColonisationDockService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lit les journaux du dossier {@code src/main/resources/exemple} et affiche les amarrages colonisation détectés.
 */
class ColonisationDocksJournalTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ColonisationDockService colonisationDockService = ColonisationDockService.getInstance();
    private final ColonisationDocksRegistry registry = ColonisationDocksRegistry.getInstance();

    @BeforeEach
    void resetRegistry() {
        registry.clear();
    }

    @Test
    void afficheListeColonisationDocksDepuisJournauxExemple() throws Exception {
        Path exempleDir = resolveExempleDir();
        if (exempleDir == null){
            return;
        }
        assertTrue(Files.isDirectory(exempleDir), "Dossier exemple introuvable: " + exempleDir.toAbsolutePath());

        try (Stream<Path> paths = Files.walk(exempleDir)) {
            paths.filter(p -> p.getFileName().toString().startsWith("Journal."))
                    .filter(p -> p.toString().endsWith(".log"))
                    .sorted()
                    .forEach(this::parseJournalFile);
        }

        System.out.println("=== Amarrages colonisation (" + registry.getDocks().size() + " entrée(s)) ===");
        for (ColonisationDockEntry e : registry.getDocks()) {
            System.out.println(formatEntry(e));
        }
    }

    private void parseJournalFile(Path journalFile) {
        try {
            for (String line : Files.readAllLines(journalFile, StandardCharsets.UTF_8)) {
                String t = line.trim();
                if (!t.startsWith("{")) {
                    continue;
                }
                JsonNode node = MAPPER.readTree(t);
                if (!"Docked".equals(node.path("event").asText())) {
                    continue;
                }
                colonisationDockService.handleDocked(node);
            }
        } catch (Exception ex) {
            System.err.println("Ignoré (lecture JSON): " + journalFile + " — " + ex.getMessage());
        }
    }

    private static String formatEntry(ColonisationDockEntry e) {
        return String.format(
                "%s | %s | %s | MarketID=%d | %s | faction=%s | %.1f LS",
                e.getTimestamp(),
                e.getStarSystem(),
                e.getSiteNameLocalised(),
                e.getMarketId(),
                e.getStationType(),
                e.getStationFactionName(),
                e.getDistFromStarLs()
        );
    }

    private static Path resolveExempleDir() {
        Path fromModule = Path.of("C:\\Users\\ewen_\\Saved Games\\Frontier Developments\\Elite Dangerous");
        if (Files.isDirectory(fromModule)) {
            return fromModule;
        }
        return null;
    }
}
