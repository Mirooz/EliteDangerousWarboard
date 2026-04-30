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
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void timestampEstToujoursLeDernierPlusUneSeconde() throws Exception {
        Path log = tempDir.resolve("Journal.2026-04-30T100000.01.log");
        Files.writeString(log, """
                { "timestamp":"2026-04-30T10:00:00Z", "event":"FSDJump", "StarSystem":"Swoilz EY-G b41-0", "SystemAddress":684105803617 }
                """, StandardCharsets.UTF_8);

        ExplorationJournalEventSimulatorTool tool = new ExplorationJournalEventSimulatorTool(new Random(7));
        tool.simulateAndAppend("scan", tempDir, Map.of("timestamp", "2099-01-01T00:00:00Z"));

        JsonNode appended = readLastJsonLine(log);
        assertEquals("2026-04-30T10:00:01Z", appended.path("timestamp").asText());
    }

    @Test
    void premierScanDuSystemeEstToujoursUneEtoileAvecNomCoherent() throws Exception {
        Path log = tempDir.resolve("Journal.2026-04-30T100000.01.log");
        Files.writeString(log, """
                { "timestamp":"2026-04-30T10:00:00Z", "event":"FSDJump", "StarSystem":"Swoilz EY-G b41-0", "SystemAddress":684105803617 }
                """, StandardCharsets.UTF_8);

        ExplorationJournalEventSimulatorTool tool = new ExplorationJournalEventSimulatorTool(new Random(9));
        tool.simulateAndAppend("scan", tempDir, Map.of());

        JsonNode appended = readLastJsonLine(log);
        assertEquals("Scan", appended.path("event").asText());
        assertTrue(appended.has("StarType"));
        assertTrue(Pattern.compile("^Swoilz EY-G b41-0 [A-Z]$").matcher(appended.path("BodyName").asText()).matches());
    }

    @Test
    void scansSuivantsRespectentUneHierarchieClassiqueSansSublune() throws Exception {
        Path log = tempDir.resolve("Journal.2026-04-30T100000.01.log");
        Files.writeString(log, """
                { "timestamp":"2026-04-30T10:00:00Z", "event":"FSDJump", "StarSystem":"Swoilz EY-G b41-0", "SystemAddress":684105803617 }
                """, StandardCharsets.UTF_8);

        ExplorationJournalEventSimulatorTool tool = new ExplorationJournalEventSimulatorTool(new Random(2));
        tool.simulateAndAppend("scan", tempDir, Map.of()); // first: star
        tool.simulateAndAppend("scan", tempDir, Map.of());
        tool.simulateAndAppend("scan", tempDir, Map.of());
        tool.simulateAndAppend("scan", tempDir, Map.of());

        List<JsonNode> scans = readScanLines(log);
        assertTrue(scans.size() >= 4);
        JsonNode first = scans.get(0);
        assertTrue(first.has("StarType"));

        for (int i = 1; i < scans.size(); i++) {
            JsonNode scan = scans.get(i);
            String bodyName = scan.path("BodyName").asText();
            JsonNode parents = scan.path("Parents");

            if (scan.has("StarType")) {
                assertTrue(Pattern.compile("^Swoilz EY-G b41-0 [A-Z]$").matcher(bodyName).matches());
                continue;
            }

            if (!parents.isArray() || parents.isEmpty()) {
                throw new AssertionError("Parents manquant pour body " + bodyName);
            }

            boolean hasPlanetParent = false;
            for (JsonNode parent : parents) {
                if (parent.has("Planet")) {
                    hasPlanetParent = true;
                    break;
                }
            }
            if (hasPlanetParent) {
                // lune: "<system> <StarLetter> <n> <letter>"
                assertTrue(Pattern.compile("^Swoilz EY-G b41-0 [A-Z] \\d+ [a-z]$").matcher(bodyName).matches());
            } else {
                // planète: "<system> <StarLetter> <n>"
                assertTrue(Pattern.compile("^Swoilz EY-G b41-0 [A-Z] \\d+$").matcher(bodyName).matches());
            }
        }
    }

    @Test
    void dockedEtScanOrganicExigentUnePlaneteSelectionnee() throws Exception {
        Path log = tempDir.resolve("Journal.2026-04-30T100000.01.log");
        Files.writeString(log, """
                { "timestamp":"2026-04-30T10:00:00Z", "event":"FSDJump", "StarSystem":"Swoilz EY-G b41-0", "SystemAddress":684105803617 }
                { "timestamp":"2026-04-30T10:00:01Z", "event":"Scan", "ScanType":"AutoScan", "BodyName":"Swoilz EY-G b41-0 A", "BodyID":1, "Parents":[ {"Null":0} ], "StarSystem":"Swoilz EY-G b41-0", "SystemAddress":684105803617, "DistanceFromArrivalLS":0.0, "StarType":"M", "Subclass":3, "StellarMass":0.3, "Radius":300000000.0, "AbsoluteMagnitude":9.1, "Age_MY":1000, "SurfaceTemperature":3000.0, "Luminosity":"V", "SemiMajorAxis":1000000000.0, "Eccentricity":0.0, "OrbitalInclination":0.0, "Periapsis":0.0, "OrbitalPeriod":1000.0, "AscendingNode":0.0, "MeanAnomaly":0.0, "RotationPeriod":1000.0, "AxialTilt":0.0, "WasDiscovered":false, "WasMapped":false, "WasFootfalled":false }
                """, StandardCharsets.UTF_8);

        ExplorationJournalEventSimulatorTool tool = new ExplorationJournalEventSimulatorTool(new Random(3));
        assertThrows(IllegalArgumentException.class, () -> tool.simulateAndAppend("docked", tempDir, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> tool.simulateAndAppend("scanorganic", tempDir, Map.of()));
    }

    @Test
    void dockedEtScanOrganicUtilisentLaPlaneteSelectionnee() throws Exception {
        Path log = tempDir.resolve("Journal.2026-04-30T100000.01.log");
        Files.writeString(log, """
                { "timestamp":"2026-04-30T10:00:00Z", "event":"FSDJump", "StarSystem":"Swoilz EY-G b41-0", "SystemAddress":684105803617 }
                { "timestamp":"2026-04-30T10:00:01Z", "event":"Scan", "ScanType":"Detailed", "BodyName":"Swoilz EY-G b41-0 A 1", "BodyID":12, "Parents":[ {"Star":1}, {"Null":0} ], "StarSystem":"Swoilz EY-G b41-0", "SystemAddress":684105803617, "DistanceFromArrivalLS":250.0, "TidalLock":true, "TerraformState":"", "PlanetClass":"Rocky body", "Atmosphere":"", "AtmosphereType":"None", "Volcanism":"", "MassEM":0.01, "Radius":1000000.0, "SurfaceGravity":1.0, "SurfaceTemperature":200.0, "SurfacePressure":0.0, "Landable":true, "Materials":[ {"Name":"iron", "Percent":20.0} ], "Composition":{"Ice":0.0, "Rock":0.7, "Metal":0.3}, "SemiMajorAxis":1000.0, "Eccentricity":0.0, "OrbitalInclination":0.0, "Periapsis":0.0, "OrbitalPeriod":1000.0, "AscendingNode":0.0, "MeanAnomaly":0.0, "RotationPeriod":1000.0, "AxialTilt":0.0, "WasDiscovered":false, "WasMapped":false, "WasFootfalled":false }
                """, StandardCharsets.UTF_8);

        ExplorationJournalEventSimulatorTool tool = new ExplorationJournalEventSimulatorTool(new Random(6));
        tool.simulateAndAppend("docked", tempDir, Map.of("selected-body-id", "12"));
        JsonNode docked = readLastJsonLine(log);
        assertEquals("Docked", docked.path("event").asText());
        assertEquals("Swoilz EY-G b41-0 A 1", docked.path("Body").asText());
        assertEquals(12, docked.path("BodyID").asInt());

        tool.simulateAndAppend("scanorganic", tempDir, Map.of("selected-body-id", "12"));
        JsonNode organic = readLastJsonLine(log);
        assertEquals("ScanOrganic", organic.path("event").asText());
        assertEquals(684105803617L, organic.path("SystemAddress").asLong());
        assertEquals(12, organic.path("Body").asInt());
    }

    @Test
    void scanOrganicFaitCycleSampleSampleAnalysePuisChangeDEspece() throws Exception {
        Path log = tempDir.resolve("Journal.2026-04-30T100000.01.log");
        Files.writeString(log, """
                { "timestamp":"2026-04-30T10:00:00Z", "event":"FSDJump", "StarSystem":"Swoilz EY-G b41-0", "SystemAddress":684105803617 }
                { "timestamp":"2026-04-30T10:00:01Z", "event":"Scan", "ScanType":"Detailed", "BodyName":"Swoilz EY-G b41-0 A 1", "BodyID":12, "Parents":[ {"Star":1}, {"Null":0} ], "StarSystem":"Swoilz EY-G b41-0", "SystemAddress":684105803617, "DistanceFromArrivalLS":250.0, "TidalLock":true, "TerraformState":"", "PlanetClass":"Rocky body", "Atmosphere":"", "AtmosphereType":"None", "Volcanism":"", "MassEM":0.01, "Radius":1000000.0, "SurfaceGravity":1.0, "SurfaceTemperature":200.0, "SurfacePressure":0.0, "Landable":true, "Materials":[ {"Name":"iron", "Percent":20.0} ], "Composition":{"Ice":0.0, "Rock":0.7, "Metal":0.3}, "SemiMajorAxis":1000.0, "Eccentricity":0.0, "OrbitalInclination":0.0, "Periapsis":0.0, "OrbitalPeriod":1000.0, "AscendingNode":0.0, "MeanAnomaly":0.0, "RotationPeriod":1000.0, "AxialTilt":0.0, "WasDiscovered":false, "WasMapped":false, "WasFootfalled":false }
                """, StandardCharsets.UTF_8);

        ExplorationJournalEventSimulatorTool tool = new ExplorationJournalEventSimulatorTool(new Random(11));

        tool.simulateAndAppend("scanorganic", tempDir, Map.of("selected-body-id", "12"));
        JsonNode e1 = readLastJsonLine(log);
        String species1 = e1.path("Species").asText();
        String variant1 = e1.path("Variant").asText();
        assertEquals("Sample", e1.path("ScanType").asText());

        tool.simulateAndAppend("scanorganic", tempDir, Map.of("selected-body-id", "12"));
        JsonNode e2 = readLastJsonLine(log);
        assertEquals("Sample", e2.path("ScanType").asText());
        assertEquals(species1, e2.path("Species").asText());
        assertEquals(variant1, e2.path("Variant").asText());

        tool.simulateAndAppend("scanorganic", tempDir, Map.of("selected-body-id", "12"));
        JsonNode e3 = readLastJsonLine(log);
        assertEquals("Analyse", e3.path("ScanType").asText());
        assertEquals(species1, e3.path("Species").asText());
        assertEquals(variant1, e3.path("Variant").asText());

        tool.simulateAndAppend("scanorganic", tempDir, Map.of("selected-body-id", "12"));
        JsonNode e4 = readLastJsonLine(log);
        assertEquals("Sample", e4.path("ScanType").asText());
        assertNotEquals(species1, e4.path("Species").asText());
    }

    @Test
    void fssBodySignalsEtSaaSignalsFoundSontLiesALaPlaneteSelectionnee() throws Exception {
        Path log = tempDir.resolve("Journal.2026-04-30T100000.01.log");
        Files.writeString(log, """
                { "timestamp":"2026-04-30T10:00:00Z", "event":"FSDJump", "StarSystem":"Swoilz EY-G b41-0", "SystemAddress":684105803617 }
                { "timestamp":"2026-04-30T10:00:01Z", "event":"Scan", "ScanType":"Detailed", "BodyName":"Swoilz EY-G b41-0 A 1", "BodyID":12, "Parents":[ {"Star":1}, {"Null":0} ], "StarSystem":"Swoilz EY-G b41-0", "SystemAddress":684105803617, "DistanceFromArrivalLS":250.0, "TidalLock":true, "TerraformState":"", "PlanetClass":"Rocky body", "Atmosphere":"", "AtmosphereType":"None", "Volcanism":"", "MassEM":0.01, "Radius":1000000.0, "SurfaceGravity":1.0, "SurfaceTemperature":200.0, "SurfacePressure":0.0, "Landable":true, "Materials":[ {"Name":"iron", "Percent":20.0} ], "Composition":{"Ice":0.0, "Rock":0.7, "Metal":0.3}, "SemiMajorAxis":1000.0, "Eccentricity":0.0, "OrbitalInclination":0.0, "Periapsis":0.0, "OrbitalPeriod":1000.0, "AscendingNode":0.0, "MeanAnomaly":0.0, "RotationPeriod":1000.0, "AxialTilt":0.0, "WasDiscovered":false, "WasMapped":false, "WasFootfalled":false }
                """, StandardCharsets.UTF_8);

        ExplorationJournalEventSimulatorTool tool = new ExplorationJournalEventSimulatorTool(new Random(8));
        tool.simulateAndAppend("fssbodysignals", tempDir, Map.of("selected-body-id", "12"));
        JsonNode fss = readLastJsonLine(log);
        assertEquals("FSSBodySignals", fss.path("event").asText());
        assertEquals("Swoilz EY-G b41-0 A 1", fss.path("BodyName").asText());
        assertEquals(12, fss.path("BodyID").asInt());
        assertEquals(684105803617L, fss.path("SystemAddress").asLong());

        tool.simulateAndAppend("saasignalsfound", tempDir, Map.of("selected-body-id", "12"));
        JsonNode saa = readLastJsonLine(log);
        assertEquals("SAASignalsFound", saa.path("event").asText());
        assertEquals("Swoilz EY-G b41-0 A 1", saa.path("BodyName").asText());
        assertEquals(12, saa.path("BodyID").asInt());
        assertEquals(684105803617L, saa.path("SystemAddress").asLong());
        assertTrue(saa.path("Genuses").isArray());
        assertTrue(saa.path("Genuses").size() > 0);
    }

    @Test
    void fssBodySignalsEtSaaSignalsFoundExigentPlaneteSelectionnee() throws Exception {
        Path log = tempDir.resolve("Journal.2026-04-30T100000.01.log");
        Files.writeString(log, """
                { "timestamp":"2026-04-30T10:00:00Z", "event":"FSDJump", "StarSystem":"Swoilz EY-G b41-0", "SystemAddress":684105803617 }
                { "timestamp":"2026-04-30T10:00:01Z", "event":"Scan", "ScanType":"AutoScan", "BodyName":"Swoilz EY-G b41-0 A", "BodyID":1, "Parents":[ {"Null":0} ], "StarSystem":"Swoilz EY-G b41-0", "SystemAddress":684105803617, "DistanceFromArrivalLS":0.0, "StarType":"M", "Subclass":3, "StellarMass":0.3, "Radius":300000000.0, "AbsoluteMagnitude":9.1, "Age_MY":1000, "SurfaceTemperature":3000.0, "Luminosity":"V", "SemiMajorAxis":1000000000.0, "Eccentricity":0.0, "OrbitalInclination":0.0, "Periapsis":0.0, "OrbitalPeriod":1000.0, "AscendingNode":0.0, "MeanAnomaly":0.0, "RotationPeriod":1000.0, "AxialTilt":0.0, "WasDiscovered":false, "WasMapped":false, "WasFootfalled":false }
                """, StandardCharsets.UTF_8);

        ExplorationJournalEventSimulatorTool tool = new ExplorationJournalEventSimulatorTool(new Random(4));
        assertThrows(IllegalArgumentException.class, () -> tool.simulateAndAppend("fssbodysignals", tempDir, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> tool.simulateAndAppend("saasignalsfound", tempDir, Map.of()));
    }

    @Test
    void ecritureSeFaitDansLeDernierJournalLexicographique() throws Exception {
        Path oldLog = tempDir.resolve("Journal.2026-04-30T090000.01.log");
        Path latestLog = tempDir.resolve("Journal.2026-04-30T100000.01.log");
        Files.writeString(oldLog, "{ \"timestamp\":\"2026-04-30T09:00:00Z\", \"event\":\"Fileheader\" }\n", StandardCharsets.UTF_8);
        Files.writeString(latestLog, """
                { "timestamp":"2026-04-30T10:00:00Z", "event":"Fileheader" }
                { "timestamp":"2026-04-30T10:00:01Z", "event":"Scan", "ScanType":"Detailed", "BodyName":"Swoilz EY-G b41-0 A 1", "BodyID":12, "Parents":[ {"Star":1}, {"Null":0} ], "StarSystem":"Swoilz EY-G b41-0", "SystemAddress":684105803617, "DistanceFromArrivalLS":250.0, "TidalLock":true, "TerraformState":"", "PlanetClass":"Rocky body", "Atmosphere":"", "AtmosphereType":"None", "Volcanism":"", "MassEM":0.01, "Radius":1000000.0, "SurfaceGravity":1.0, "SurfaceTemperature":200.0, "SurfacePressure":0.0, "Landable":true, "Materials":[ {"Name":"iron", "Percent":20.0} ], "Composition":{"Ice":0.0, "Rock":0.7, "Metal":0.3}, "SemiMajorAxis":1000.0, "Eccentricity":0.0, "OrbitalInclination":0.0, "Periapsis":0.0, "OrbitalPeriod":1000.0, "AscendingNode":0.0, "MeanAnomaly":0.0, "RotationPeriod":1000.0, "AxialTilt":0.0, "WasDiscovered":false, "WasMapped":false, "WasFootfalled":false }
                """, StandardCharsets.UTF_8);

        ExplorationJournalEventSimulatorTool tool = new ExplorationJournalEventSimulatorTool(new Random(2));
        Path updated = tool.simulateAndAppend("docked", tempDir, Map.of("selected-body-id", "12"));

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

    private static List<JsonNode> readScanLines(Path file) throws Exception {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        return lines.stream()
                .map(line -> {
                    try {
                        return MAPPER.readTree(line);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .filter(node -> "Scan".equals(node.path("event").asText()))
                .toList();
    }
}
