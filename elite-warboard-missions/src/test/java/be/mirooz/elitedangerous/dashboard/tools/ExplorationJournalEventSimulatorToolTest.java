package be.mirooz.elitedangerous.dashboard.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplorationJournalEventSimulatorToolTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @TempDir
    Path tempDir;

    @Test
    void fsdJumpGenereUnNouveauSystemeDifferentDuCourant() throws Exception {
        Path log = tempDir.resolve("Journal.2026-04-30T100000.01.log");
        Files.writeString(log, """
                { "timestamp":"2026-04-30T10:00:00Z", "event":"FSDJump", "StarSystem":"Swoilz RE-U c18-3", "SystemAddress":910096405410 }
                """, StandardCharsets.UTF_8);

        ExplorationJournalEventSimulatorTool tool = new ExplorationJournalEventSimulatorTool(new Random(42));
        tool.simulateAndAppend("fsdjump", tempDir, Map.of());

        JsonNode appended = readLastJsonLine(log);
        assertEquals("FSDJump", appended.path("event").asText());
        assertNotEquals("Swoilz RE-U c18-3", appended.path("StarSystem").asText());
        assertNotEquals(910096405410L, appended.path("SystemAddress").asLong());
    }

    @Test
    void scanReprendLeSystemeEtLAdresseCourants() throws Exception {
        Path log = tempDir.resolve("Journal.2026-04-30T100000.01.log");
        Files.writeString(log, """
                { "timestamp":"2026-04-30T10:00:00Z", "event":"FSDJump", "StarSystem":"Swoilz EY-G b41-0", "SystemAddress":684105803617 }
                """, StandardCharsets.UTF_8);

        ExplorationJournalEventSimulatorTool tool = new ExplorationJournalEventSimulatorTool(new Random(7));
        tool.simulateAndAppend("scan", tempDir, Map.of());

        JsonNode appended = readLastJsonLine(log);
        assertEquals("Scan", appended.path("event").asText());
        assertEquals("Swoilz EY-G b41-0", appended.path("StarSystem").asText());
        assertEquals(684105803617L, appended.path("SystemAddress").asLong());
    }

    @Test
    void scanOrganicCibleLaPlaneteDuDernierSystemeDocke() throws Exception {
        Path log = tempDir.resolve("Journal.2026-04-30T100000.01.log");
        Files.writeString(log, """
                { "timestamp":"2026-04-30T10:00:00Z", "event":"Scan", "BodyName":"Other System A 1", "BodyID":1, "StarSystem":"Other System", "SystemAddress":111, "PlanetClass":"Rocky body" }
                { "timestamp":"2026-04-30T10:01:00Z", "event":"Scan", "BodyName":"Docked System A 7", "BodyID":7, "StarSystem":"Docked System", "SystemAddress":222, "PlanetClass":"Rocky body" }
                { "timestamp":"2026-04-30T10:02:00Z", "event":"Docked", "StarSystem":"Docked System", "SystemAddress":222, "StationName":"Port Example", "MarketID":123 }
                """, StandardCharsets.UTF_8);

        ExplorationJournalEventSimulatorTool tool = new ExplorationJournalEventSimulatorTool(new Random(9));
        tool.simulateAndAppend("scanorganic", tempDir, Map.of());

        JsonNode appended = readLastJsonLine(log);
        assertEquals("ScanOrganic", appended.path("event").asText());
        assertEquals(222L, appended.path("SystemAddress").asLong());
        assertEquals(7, appended.path("Body").asInt());
    }

    @Test
    void ecritureSeFaitDansLeDernierJournalLexicographique() throws Exception {
        Path oldLog = tempDir.resolve("Journal.2026-04-30T090000.01.log");
        Path latestLog = tempDir.resolve("Journal.2026-04-30T100000.01.log");
        Files.writeString(oldLog, "{ \"timestamp\":\"2026-04-30T09:00:00Z\", \"event\":\"Fileheader\" }\n", StandardCharsets.UTF_8);
        Files.writeString(latestLog, "{ \"timestamp\":\"2026-04-30T10:00:00Z\", \"event\":\"Fileheader\" }\n", StandardCharsets.UTF_8);

        ExplorationJournalEventSimulatorTool tool = new ExplorationJournalEventSimulatorTool(new Random(2));
        Path updated = tool.simulateAndAppend("docked", tempDir, Map.of());

        assertEquals(latestLog, updated);
        List<String> oldLines = Files.readAllLines(oldLog, StandardCharsets.UTF_8);
        List<String> latestLines = Files.readAllLines(latestLog, StandardCharsets.UTF_8);
        assertEquals(1, oldLines.size());
        assertTrue(latestLines.size() >= 2);
    }

    private static JsonNode readLastJsonLine(Path file) throws Exception {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        String last = lines.get(lines.size() - 1);
        return MAPPER.readTree(last);
    }
}
