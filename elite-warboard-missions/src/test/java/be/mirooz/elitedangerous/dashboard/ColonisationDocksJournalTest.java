package be.mirooz.elitedangerous.dashboard;

import be.mirooz.elitedangerous.dashboard.handlers.events.journalevents.ColonisationConstructionDepotHandler;
import be.mirooz.elitedangerous.dashboard.model.colonisation.ColonisationConstruction;
import be.mirooz.elitedangerous.dashboard.model.colonisation.ColonisationDockEntry;
import be.mirooz.elitedangerous.dashboard.model.registries.colonisation.ColonisationDocksRegistry;
import be.mirooz.elitedangerous.dashboard.service.ColonisationDockService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Lit les journaux et affiche les {@link ColonisationDockEntry} (dock + {@link ColonisationConstruction}).
 */
class ColonisationDocksJournalTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ColonisationDockService colonisationDockService = ColonisationDockService.getInstance();
    private final ColonisationConstructionDepotHandler constructionDepotHandler = new ColonisationConstructionDepotHandler();
    private final ColonisationDocksRegistry registry = ColonisationDocksRegistry.getInstance();

    @BeforeEach
    void resetRegistry() {
        registry.clear();
    }

    @Test
    void afficheListeColonisationDocksDepuisJournauxExemple() throws Exception {
        Path journalDir = resolveJournalDir();
        Assumptions.assumeTrue(journalDir != null && Files.isDirectory(journalDir),
                "Aucun dossier journaux trouvé (Saved Games ou resources/exemple)");

        try (Stream<Path> paths = Files.walk(journalDir)) {
            paths.filter(p -> p.getFileName().toString().startsWith("Journal."))
                    .filter(p -> p.toString().endsWith(".log"))
                    .sorted()
                    .forEach(this::parseJournalFile);
        }

        System.out.println("=== ColonisationDockEntry (" + registry.getDockEntries().size() + " MarketID) ===");
        for (ColonisationDockEntry e : registry.getDockEntries()) {
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
                String event = node.path("event").asText();
                if ("Docked".equals(event)) {
                    colonisationDockService.handleDocked(node);
                } else if ("ColonisationConstructionDepot".equals(event)) {
                    constructionDepotHandler.handle(node);
                }
            }
        } catch (Exception ex) {
            System.err.println("Ignoré (lecture JSON): " + journalFile + " — " + ex.getMessage());
        }
    }

    private static String formatEntry(ColonisationDockEntry e) {
        String dock = e.getDockTimestamp() != null && !e.getDockTimestamp().isEmpty()
                ? e.getDockTimestamp() + " | " + nullToEmpty(e.getStarSystem()) + " | " + nullToEmpty(e.getSiteNameLocalised())
                : "(pas encore d'amarrage Docked)";
        ColonisationConstruction c = e.getConstruction();
        String depot = c != null
                ? String.format("construction %s | progress=%.4f | status=%s | %d ressource(s)",
                c.getEventTimestamp(),
                c.getConstructionProgress(),
                c.getStatus(),
                c.getResourcesRequired().size())
                : "(pas encore de ColonisationConstructionDepot)";
        return String.format("MarketID=%d | %s || %s", e.getMarketId(), dock, depot);
    }

    private static String nullToEmpty(String v) {
        return v == null ? "" : v;
    }

    private static Path resolveJournalDir() {
        Path saved = Path.of(System.getProperty("user.home"), "Saved Games", "Frontier Developments", "Elite Dangerous");
        if (Files.isDirectory(saved)) {
            return saved;
        }
        Path fromModule = Path.of("src/main/resources/exemple");
        if (Files.isDirectory(fromModule)) {
            return fromModule;
        }
        Path fromRepoRoot = Path.of("elite-warboard-missions/src/main/resources/exemple");
        if (Files.isDirectory(fromRepoRoot)) {
            return fromRepoRoot;
        }
        return null;
    }
}
