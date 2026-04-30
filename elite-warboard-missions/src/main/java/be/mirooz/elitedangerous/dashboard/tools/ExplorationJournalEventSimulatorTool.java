package be.mirooz.elitedangerous.dashboard.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;

/**
 * Outil CLI de simulation des événements journal orientés exploration.
 * <p>
 * Écrit une ligne JSON dans le dernier Journal.*.log existant.
 */
public class ExplorationJournalEventSimulatorTool {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String EVENT_FSD_JUMP = "fsdjump";
    private static final String EVENT_SCAN = "scan";
    private static final String EVENT_SCAN_ORGANIC = "scanorganic";
    private static final String EVENT_DOCKED = "docked";

    private static final List<String> SYSTEM_PREFIXES = List.of(
            "Swoilz", "Dryooe", "Bleia", "Hypua", "Prua", "Trifid"
    );

    private final Random random;

    public ExplorationJournalEventSimulatorTool() {
        this(new Random());
    }

    ExplorationJournalEventSimulatorTool(Random random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    public static void main(String[] args) {
        try {
            CliInput input = CliInput.parse(args);
            if (input == null) {
                printUsage();
                return;
            }

            ExplorationJournalEventSimulatorTool tool = new ExplorationJournalEventSimulatorTool();
            Path journal = tool.simulateAndAppend(input.eventType(), input.journalDirectory(), input.options());
            System.out.println("Evenement ajoute dans " + journal + " : " + input.eventType());
        } catch (Exception e) {
            System.err.println("Erreur simulation journal: " + e.getMessage());
            printUsage();
            System.exit(1);
        }
    }

    public Path simulateAndAppend(String eventType, Path journalDirectory, Map<String, String> options) throws IOException {
        String normalizedEvent = normalizeEventType(eventType);
        Path latestJournal = findLatestJournalFile(journalDirectory);
        JournalContext context = buildContextFromJournal(latestJournal);

        ObjectNode eventNode = switch (normalizedEvent) {
            case EVENT_FSD_JUMP -> createFsdJumpEvent(context, options);
            case EVENT_SCAN -> createScanEvent(context, options);
            case EVENT_SCAN_ORGANIC -> createScanOrganicEvent(context, options);
            case EVENT_DOCKED -> createDockedEvent(context, options);
            default -> throw new IllegalArgumentException("Type d'evenement non supporte: " + eventType);
        };

        appendJsonLine(latestJournal, OBJECT_MAPPER.writeValueAsString(eventNode));
        return latestJournal;
    }

    private static String normalizeEventType(String eventType) {
        if (eventType == null) {
            throw new IllegalArgumentException("Le type d'evenement est obligatoire");
        }
        return eventType.trim().toLowerCase(Locale.ROOT);
    }

    private Path findLatestJournalFile(Path journalDirectory) throws IOException {
        if (journalDirectory == null || !Files.isDirectory(journalDirectory)) {
            throw new IllegalArgumentException("Dossier journal invalide: " + journalDirectory);
        }

        try (Stream<Path> files = Files.list(journalDirectory)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String fileName = p.getFileName().toString();
                        return fileName.startsWith("Journal.") && fileName.endsWith(".log");
                    })
                    .max(Comparator.comparing(path -> path.getFileName().toString()))
                    .orElseThrow(() -> new IllegalArgumentException("Aucun fichier Journal.*.log trouve dans " + journalDirectory));
        }
    }

    private JournalContext buildContextFromJournal(Path journalFile) throws IOException {
        JournalContext context = new JournalContext();
        List<String> lines = Files.readAllLines(journalFile, StandardCharsets.UTF_8);

        for (String rawLine : lines) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (!line.startsWith("{")) {
                continue;
            }

            JsonNode node;
            try {
                node = OBJECT_MAPPER.readTree(line);
            } catch (Exception ignored) {
                continue;
            }

            context.lastTimestamp = extractTimestamp(node).orElse(context.lastTimestamp);
            String event = node.path("event").asText("");

            if ("FSDJump".equals(event) || "Location".equals(event) || "Docked".equals(event)) {
                updateCurrentSystem(context, node);
            }
            if ("Docked".equals(event)) {
                context.lastDockedStarSystem = textValue(node, "StarSystem");
                context.lastDockedSystemAddress = node.path("SystemAddress").asLong(0L);
            }

            updateMaxBodyIdFromNode(context, node);

            if ("Scan".equals(event) && node.has("PlanetClass")) {
                buildPlanetFromScan(node).ifPresent(planet -> registerPlanet(context, planet));
            } else if (isPlanetProximityEvent(event, node)) {
                buildPlanetFromGenericBodyEvent(node).ifPresent(planet -> registerPlanet(context, planet));
            }
        }

        if (context.currentStarSystem == null && context.lastScannedPlanet != null) {
            context.currentStarSystem = context.lastScannedPlanet.starSystem;
            context.currentSystemAddress = context.lastScannedPlanet.systemAddress;
        }
        if (context.currentSystemAddress <= 0 && context.currentStarSystem != null) {
            PlanetContext planet = context.lastPlanetBySystem.get(key(context.currentStarSystem));
            if (planet != null) {
                context.currentSystemAddress = planet.systemAddress;
            }
        }
        return context;
    }

    private static Optional<Instant> extractTimestamp(JsonNode node) {
        String value = textValue(node, "timestamp");
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Instant.parse(value));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private static boolean isPlanetProximityEvent(String event, JsonNode node) {
        if (!List.of("Touchdown", "SupercruiseExit", "Embark", "Disembark").contains(event)) {
            return false;
        }
        if (!node.has("BodyID") || !node.has("StarSystem")) {
            return false;
        }
        return "Planet".equalsIgnoreCase(node.path("BodyType").asText("")) || node.path("OnPlanet").asBoolean(false);
    }

    private static Optional<PlanetContext> buildPlanetFromScan(JsonNode node) {
        if (!node.has("BodyID") || !node.has("StarSystem")) {
            return Optional.empty();
        }
        int bodyId = node.path("BodyID").asInt(-1);
        if (bodyId < 0) {
            return Optional.empty();
        }
        String starSystem = textValue(node, "StarSystem");
        if (starSystem == null || starSystem.isBlank()) {
            return Optional.empty();
        }
        long systemAddress = node.path("SystemAddress").asLong(0L);
        String bodyName = textValue(node, "BodyName");
        if (bodyName == null || bodyName.isBlank()) {
            bodyName = starSystem + " Body " + bodyId;
        }
        return Optional.of(new PlanetContext(bodyName, bodyId, starSystem, systemAddress));
    }

    private static Optional<PlanetContext> buildPlanetFromGenericBodyEvent(JsonNode node) {
        int bodyId = node.path("BodyID").asInt(-1);
        if (bodyId < 0) {
            return Optional.empty();
        }
        String starSystem = textValue(node, "StarSystem");
        if (starSystem == null || starSystem.isBlank()) {
            return Optional.empty();
        }
        long systemAddress = node.path("SystemAddress").asLong(0L);
        String bodyName = textValue(node, "Body");
        if (bodyName == null || bodyName.isBlank()) {
            bodyName = starSystem + " Body " + bodyId;
        }
        return Optional.of(new PlanetContext(bodyName, bodyId, starSystem, systemAddress));
    }

    private static void registerPlanet(JournalContext context, PlanetContext planet) {
        context.lastScannedPlanet = planet;
        context.lastPlanetBySystem.put(key(planet.starSystem), planet);
        int currentMax = context.maxBodyIdBySystem.getOrDefault(key(planet.starSystem), 0);
        context.maxBodyIdBySystem.put(key(planet.starSystem), Math.max(currentMax, planet.bodyId));
    }

    private static void updateCurrentSystem(JournalContext context, JsonNode node) {
        String starSystem = textValue(node, "StarSystem");
        if (starSystem != null && !starSystem.isBlank()) {
            context.currentStarSystem = starSystem;
        }
        long address = node.path("SystemAddress").asLong(0L);
        if (address > 0) {
            context.currentSystemAddress = address;
        }
    }

    private static void updateMaxBodyIdFromNode(JournalContext context, JsonNode node) {
        String starSystem = textValue(node, "StarSystem");
        int bodyId = node.path("BodyID").asInt(-1);
        if (starSystem == null || starSystem.isBlank() || bodyId < 0) {
            return;
        }
        String systemKey = key(starSystem);
        int currentMax = context.maxBodyIdBySystem.getOrDefault(systemKey, 0);
        context.maxBodyIdBySystem.put(systemKey, Math.max(currentMax, bodyId));
    }

    private ObjectNode createFsdJumpEvent(JournalContext context, Map<String, String> options) {
        String currentSystem = context.currentStarSystem == null ? "Simulation Seed" : context.currentStarSystem;
        String newSystem = options.getOrDefault("system", generateNextSystemName(currentSystem));
        if (newSystem.equalsIgnoreCase(currentSystem)) {
            newSystem = newSystem + " Next";
        }
        long systemAddress = parseLongOption(options, "system-address")
                .orElseGet(() -> generateDifferentAddress(context.currentSystemAddress));

        ObjectNode node = createBaseEvent("FSDJump", context, options);
        node.put("Taxi", false);
        node.put("Multicrew", false);
        node.put("StarSystem", newSystem);
        node.put("SystemAddress", systemAddress);

        ArrayNode starPos = node.putArray("StarPos");
        starPos.add(round(randomInRange(-1400.0, 1400.0), 5));
        starPos.add(round(randomInRange(-900.0, 900.0), 5));
        starPos.add(round(randomInRange(-1500.0, 1500.0), 5));

        node.put("SystemAllegiance", "");
        node.put("SystemEconomy", "$economy_None;");
        node.put("SystemEconomy_Localised", "Aucune");
        node.put("SystemSecondEconomy", "$economy_None;");
        node.put("SystemSecondEconomy_Localised", "Aucune");
        node.put("SystemGovernment", "$government_None;");
        node.put("SystemGovernment_Localised", "Aucune");
        node.put("SystemSecurity", "$GAlAXY_MAP_INFO_state_anarchy;");
        node.put("SystemSecurity_Localised", "Anarchie");
        node.put("Population", 0);
        node.put("Body", newSystem + " A");
        node.put("BodyID", 1);
        node.put("BodyType", "Star");
        node.put("JumpDist", round(randomInRange(18.0, 82.0), 3));
        node.put("FuelUsed", round(randomInRange(2.1, 5.8), 6));
        node.put("FuelLevel", round(randomInRange(8.0, 31.0), 6));
        return node;
    }

    private ObjectNode createScanEvent(JournalContext context, Map<String, String> options) {
        String starSystem = options.getOrDefault("system", defaultSystemName(context));
        long systemAddress = parseLongOption(options, "system-address")
                .orElseGet(() -> resolveSystemAddressForSystem(context, starSystem));

        int bodyId = parseIntOption(options, "body-id")
                .orElseGet(() -> nextBodyIdForSystem(context, starSystem));
        String bodyName = options.getOrDefault("body-name", starSystem + " A " + bodyId);

        ObjectNode node = createBaseEvent("Scan", context, options);
        node.put("ScanType", options.getOrDefault("scan-type", "Detailed"));
        node.put("BodyName", bodyName);
        node.put("BodyID", bodyId);

        ArrayNode parents = node.putArray("Parents");
        parents.addObject().put("Star", 1);
        parents.addObject().put("Null", 0);

        node.put("StarSystem", starSystem);
        node.put("SystemAddress", systemAddress);
        node.put("DistanceFromArrivalLS", round(randomInRange(30.0, 3200.0), 6));
        node.put("TidalLock", random.nextBoolean());
        node.put("TerraformState", "");
        node.put("PlanetClass", options.getOrDefault("planet-class", "High metal content body"));
        node.put("Atmosphere", "");
        node.put("AtmosphereType", "None");
        node.put("Volcanism", "");
        node.put("MassEM", round(randomInRange(0.001, 0.008), 6));
        node.put("Radius", round(randomInRange(650000.0, 1450000.0), 6));
        node.put("SurfaceGravity", round(randomInRange(0.95, 2.15), 6));
        node.put("SurfaceTemperature", round(randomInRange(90.0, 610.0), 6));
        node.put("SurfacePressure", 0.0);
        node.put("Landable", true);

        ArrayNode materials = node.putArray("Materials");
        materials.addObject()
                .put("Name", "iron")
                .put("Name_Localised", "Fer")
                .put("Percent", round(randomInRange(18.0, 25.0), 6));
        materials.addObject()
                .put("Name", "nickel")
                .put("Percent", round(randomInRange(12.0, 18.0), 6));
        materials.addObject()
                .put("Name", "sulphur")
                .put("Name_Localised", "Soufre")
                .put("Percent", round(randomInRange(9.0, 17.0), 6));

        ObjectNode composition = node.putObject("Composition");
        composition.put("Ice", 0.0);
        composition.put("Rock", round(randomInRange(0.60, 0.75), 6));
        composition.put("Metal", round(1.0 - composition.path("Rock").asDouble(), 6));

        node.put("SemiMajorAxis", randomInRange(1.2E9, 7.9E9));
        node.put("Eccentricity", round(randomInRange(0.0002, 0.0087), 6));
        node.put("OrbitalInclination", round(randomInRange(-1.5, 1.5), 6));
        node.put("Periapsis", round(randomInRange(10.0, 350.0), 6));
        node.put("OrbitalPeriod", randomInRange(150000.0, 1650000.0));
        node.put("AscendingNode", round(randomInRange(-170.0, 170.0), 6));
        node.put("MeanAnomaly", round(randomInRange(0.0, 359.9), 6));
        node.put("RotationPeriod", randomInRange(145000.0, 1700000.0));
        node.put("AxialTilt", round(randomInRange(-1.5, 1.5), 6));
        node.put("WasDiscovered", false);
        node.put("WasMapped", false);
        node.put("WasFootfalled", false);
        return node;
    }

    private ObjectNode createScanOrganicEvent(JournalContext context, Map<String, String> options) {
        PlanetContext organicTarget = resolveOrganicTargetPlanet(context)
                .orElseGet(() -> new PlanetContext(
                        defaultSystemName(context) + " A " + nextBodyIdForSystem(context, defaultSystemName(context)),
                        nextBodyIdForSystem(context, defaultSystemName(context)),
                        defaultSystemName(context),
                        resolveSystemAddressForSystem(context, defaultSystemName(context))
                ));

        int bodyId = parseIntOption(options, "body-id").orElse(organicTarget.bodyId);
        long systemAddress = parseLongOption(options, "system-address").orElse(organicTarget.systemAddress);

        ObjectNode node = createBaseEvent("ScanOrganic", context, options);
        node.put("ScanType", options.getOrDefault("scan-type", "Log"));
        node.put("Genus", "$Codex_Ent_Stratum_Genus_Name;");
        node.put("Genus_Localised", "Stratum");
        node.put("Species", "$Codex_Ent_Stratum_07_Name;");
        node.put("Species_Localised", "Stratum Tectonicas");
        node.put("Variant", "$Codex_Ent_Stratum_07_L_Name;");
        node.put("Variant_Localised", "Stratum Tectonicas - Turquoise");
        node.put("WasLogged", false);
        node.put("SystemAddress", systemAddress);
        node.put("Body", bodyId);
        return node;
    }

    private ObjectNode createDockedEvent(JournalContext context, Map<String, String> options) {
        String starSystem = options.getOrDefault("system", defaultSystemName(context));
        long systemAddress = parseLongOption(options, "system-address")
                .orElseGet(() -> resolveSystemAddressForSystem(context, starSystem));

        ObjectNode node = createBaseEvent("Docked", context, options);
        node.put("StationName", options.getOrDefault("station-name", "Warboard Simulation Port"));
        node.put("StationType", options.getOrDefault("station-type", "Coriolis"));
        node.put("Taxi", false);
        node.put("Multicrew", false);
        node.put("StarSystem", starSystem);
        node.put("SystemAddress", systemAddress);
        node.put("MarketID", parseLongOption(options, "market-id").orElse(Math.abs(random.nextLong(9_999_999_999L))));
        node.putObject("StationFaction").put("Name", "Pilots Federation Local Branch");
        node.put("StationGovernment", "$government_Democracy;");
        node.put("StationGovernment_Localised", "Democratie");
        node.put("StationAllegiance", "PilotsFederation");

        ArrayNode services = node.putArray("StationServices");
        services.add("dock");
        services.add("autodock");
        services.add("contacts");
        services.add("exploration");
        services.add("stationMenu");

        node.put("StationEconomy", "$economy_HighTech;");
        node.put("StationEconomy_Localised", "Haute Technologie");
        node.put("DistFromStarLS", round(randomInRange(15.0, 1200.0), 3));
        return node;
    }

    private Optional<PlanetContext> resolveOrganicTargetPlanet(JournalContext context) {
        if (context.lastDockedStarSystem != null && !context.lastDockedStarSystem.isBlank()) {
            PlanetContext fromDockedSystem = context.lastPlanetBySystem.get(key(context.lastDockedStarSystem));
            if (fromDockedSystem != null) {
                return Optional.of(fromDockedSystem);
            }
        }
        return Optional.ofNullable(context.lastScannedPlanet);
    }

    private ObjectNode createBaseEvent(String eventName, JournalContext context, Map<String, String> options) {
        ObjectNode node = OBJECT_MAPPER.createObjectNode();
        node.put("timestamp", options.getOrDefault("timestamp", nextTimestamp(context)));
        node.put("event", eventName);
        return node;
    }

    private String nextTimestamp(JournalContext context) {
        Instant base = context.lastTimestamp == null ? Instant.now() : context.lastTimestamp.plusSeconds(1);
        return base.truncatedTo(ChronoUnit.SECONDS).toString();
    }

    private String defaultSystemName(JournalContext context) {
        if (context.currentStarSystem != null && !context.currentStarSystem.isBlank()) {
            return context.currentStarSystem;
        }
        if (context.lastScannedPlanet != null && context.lastScannedPlanet.starSystem != null) {
            return context.lastScannedPlanet.starSystem;
        }
        return "Simulation Alpha";
    }

    private long resolveSystemAddressForSystem(JournalContext context, String starSystem) {
        if (starSystem != null && context.currentStarSystem != null
                && starSystem.equalsIgnoreCase(context.currentStarSystem)
                && context.currentSystemAddress > 0) {
            return context.currentSystemAddress;
        }
        PlanetContext bySystem = context.lastPlanetBySystem.get(key(starSystem));
        if (bySystem != null && bySystem.systemAddress > 0) {
            return bySystem.systemAddress;
        }
        if (context.lastDockedStarSystem != null && context.lastDockedStarSystem.equalsIgnoreCase(starSystem)
                && context.lastDockedSystemAddress > 0) {
            return context.lastDockedSystemAddress;
        }
        return generateDifferentAddress(context.currentSystemAddress);
    }

    private int nextBodyIdForSystem(JournalContext context, String starSystem) {
        int current = context.maxBodyIdBySystem.getOrDefault(key(starSystem), 0);
        return Math.max(1, current + 1);
    }

    private String generateNextSystemName(String currentSystem) {
        String prefix = SYSTEM_PREFIXES.get(random.nextInt(SYSTEM_PREFIXES.size()));
        String code = randomUpperCase(2) + "-" + randomUpperCase(1);
        String suffix = " " + random.nextInt(80) + "-" + random.nextInt(10);
        String candidate = prefix + " " + code + suffix;
        if (candidate.equalsIgnoreCase(currentSystem)) {
            return candidate + " A";
        }
        return candidate;
    }

    private String randomUpperCase(int size) {
        StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            sb.append((char) ('A' + random.nextInt(26)));
        }
        return sb.toString();
    }

    private long generateDifferentAddress(long existingAddress) {
        long value = Math.abs(random.nextLong(9_000_000_000_000L)) + 10_000_000_000L;
        if (value == existingAddress) {
            return value + 37L;
        }
        return value;
    }

    private static Optional<Long> parseLongOption(Map<String, String> options, String key) {
        String value = options.get(key);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Option numerique invalide --" + key + "=" + value);
        }
    }

    private static Optional<Integer> parseIntOption(Map<String, String> options, String key) {
        String value = options.get(key);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Option numerique invalide --" + key + "=" + value);
        }
    }

    private void appendJsonLine(Path journalFile, String jsonLine) throws IOException {
        StringBuilder data = new StringBuilder();
        if (Files.size(journalFile) > 0 && !endsWithLineBreak(journalFile)) {
            data.append(System.lineSeparator());
        }
        data.append(jsonLine).append(System.lineSeparator());
        Files.writeString(journalFile, data.toString(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    private boolean endsWithLineBreak(Path journalFile) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(journalFile.toFile(), "r")) {
            long length = raf.length();
            if (length == 0) {
                return false;
            }
            raf.seek(length - 1);
            int lastByte = raf.read();
            return lastByte == '\n' || lastByte == '\r';
        }
    }

    private static String key(String starSystem) {
        return starSystem == null ? "" : starSystem.toLowerCase(Locale.ROOT);
    }

    private static String textValue(JsonNode node, String fieldName) {
        if (node == null || fieldName == null) {
            return null;
        }
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text;
    }

    private double randomInRange(double min, double max) {
        return min + (max - min) * random.nextDouble();
    }

    private static double round(double value, int scale) {
        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }

    private static void printUsage() {
        System.out.println("""
                Usage:
                  mvn -pl elite-warboard-missions exec:java -Dexec.mainClass=be.mirooz.elitedangerous.dashboard.tools.ExplorationJournalEventSimulatorTool -Dexec.args="<event> [--journal-dir=PATH] [--key=value]"

                Events supportes:
                  fsdjump
                  scan
                  scanorganic
                  docked

                Exemples:
                  ... -Dexec.args="fsdjump --journal-dir=elite-warboard-missions/src/main/resources/exemple"
                  ... -Dexec.args="scan --scan-type=Detailed"
                  ... -Dexec.args="scanorganic --scan-type=Sample"
                """);
    }

    private record CliInput(String eventType, Path journalDirectory, Map<String, String> options) {
        static CliInput parse(String[] args) {
            if (args == null || args.length == 0) {
                return null;
            }
            String eventType = normalizeEventType(args[0]);
            if ("help".equals(eventType) || "--help".equals(eventType) || "-h".equals(eventType)) {
                return null;
            }

            Map<String, String> options = new HashMap<>();
            for (int i = 1; i < args.length; i++) {
                String token = args[i];
                if (token == null || token.isBlank()) {
                    continue;
                }
                if (!token.startsWith("--") || !token.contains("=")) {
                    throw new IllegalArgumentException("Argument invalide: " + token);
                }
                int eq = token.indexOf('=');
                String key = token.substring(2, eq).trim().toLowerCase(Locale.ROOT);
                String value = token.substring(eq + 1).trim();
                options.put(key, value);
            }

            Path journalDirectory = resolveJournalDirectory(options.get("journal-dir"));
            return new CliInput(eventType, journalDirectory, Map.copyOf(options));
        }

        private static Path resolveJournalDirectory(String configuredPath) {
            if (configuredPath != null && !configuredPath.isBlank()) {
                return Path.of(configuredPath);
            }

            List<Path> candidates = new ArrayList<>();
            candidates.add(Path.of("src/main/resources/exemple"));
            candidates.add(Path.of("elite-warboard-missions/src/main/resources/exemple"));
            candidates.add(Path.of(
                    System.getProperty("user.home"),
                    "Saved Games",
                    "Frontier Developments",
                    "Elite Dangerous"
            ));

            return candidates.stream()
                    .filter(Files::isDirectory)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Aucun dossier journal detecte automatiquement; utilisez --journal-dir=..."
                    ));
        }
    }

    private static final class JournalContext {
        private String currentStarSystem;
        private long currentSystemAddress;
        private String lastDockedStarSystem;
        private long lastDockedSystemAddress;
        private PlanetContext lastScannedPlanet;
        private Instant lastTimestamp;
        private final Map<String, PlanetContext> lastPlanetBySystem = new HashMap<>();
        private final Map<String, Integer> maxBodyIdBySystem = new HashMap<>();
    }

    private record PlanetContext(String bodyName, int bodyId, String starSystem, long systemAddress) {
    }
}
