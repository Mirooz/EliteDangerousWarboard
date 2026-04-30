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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private static final String EVENT_FSS_BODY_SIGNALS = "fssbodysignals";
    private static final String EVENT_SAA_SIGNALS_FOUND = "saasignalsfound";

    private static final List<String> SYSTEM_PREFIXES = List.of(
            "Swoilz", "Dryooe", "Bleia", "Hypua", "Prua", "Trifid"
    );
    private static final List<String> STAR_TYPES = List.of("M", "K", "G", "F", "A");
    private static final List<OrganicSpeciesProfile> ORGANIC_SPECIES_SEQUENCE = List.of(
            new OrganicSpeciesProfile(
                    "$Codex_Ent_Stratum_Genus_Name;",
                    "Stratum",
                    "$Codex_Ent_Stratum_07_Name;",
                    "Stratum Tectonicas",
                    "$Codex_Ent_Stratum_07_L_Name;",
                    "Stratum Tectonicas - Turquoise"
            ),
            new OrganicSpeciesProfile(
                    "$Codex_Ent_Fonticulus_Genus_Name;",
                    "Fonticulua",
                    "$Codex_Ent_Fonticulus_03_Name;",
                    "Fonticulua Upupam",
                    "$Codex_Ent_Fonticulus_03_T_Name;",
                    "Fonticulua Upupam - Orange"
            ),
            new OrganicSpeciesProfile(
                    "$Codex_Ent_Bacterial_Genus_Name;",
                    "Bacterium",
                    "$Codex_Ent_Bacterial_12_Name;",
                    "Bacterium Aurasus",
                    "$Codex_Ent_Bacterial_12_A_Name;",
                    "Bacterium Aurasus - Lime"
            ),
            new OrganicSpeciesProfile(
                    "$Codex_Ent_Shrubs_Genus_Name;",
                    "Frutexa",
                    "$Codex_Ent_Shrubs_03_Name;",
                    "Frutexa Metallicum",
                    "$Codex_Ent_Shrubs_03_B_Name;",
                    "Frutexa Metallicum - Teal"
            )
    );
    private static final List<OrganicSignature> ORGANIC_SIGNATURE_POOL = List.of(
            new OrganicSignature(
                    "$Codex_Ent_Stratum_Genus_Name;",
                    "Stratum"
            ),
            new OrganicSignature(
                    "$Codex_Ent_Fonticulus_Genus_Name;",
                    "Fonticulua"
            ),
            new OrganicSignature(
                    "$Codex_Ent_Bacterial_Genus_Name;",
                    "Bacterium"
            ),
            new OrganicSignature(
                    "$Codex_Ent_Tussocks_Genus_Name;",
                    "Touradon"
            )
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
            case EVENT_FSS_BODY_SIGNALS -> createFssBodySignalsEvent(context, options);
            case EVENT_SAA_SIGNALS_FOUND -> createSaaSignalsFoundEvent(context, options);
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

            if ("Scan".equals(event)) {
                buildBodyNodeFromScan(node, context.lastTimestamp).ifPresent(body -> {
                    SystemModel model = context.systems.computeIfAbsent(
                            key(body.starSystem),
                            k -> new SystemModel(body.starSystem, body.systemAddress)
                    );
                    if (model.systemAddress <= 0 && body.systemAddress > 0) {
                        model.systemAddress = body.systemAddress;
                    }
                    model.register(body);
                    if (body.isPlanetary()) {
                        registerPlanet(context, new PlanetContext(body.bodyName, body.bodyId, body.starSystem, body.systemAddress));
                        context.selectedPlanet = body;
                    }
                });
            } else if (isPlanetProximityEvent(event, node)) {
                buildPlanetFromGenericBodyEvent(node).ifPresent(planet -> {
                    registerPlanet(context, planet);
                    SystemModel model = context.systems.computeIfAbsent(
                            key(planet.starSystem),
                            k -> new SystemModel(planet.starSystem, planet.systemAddress)
                    );
                    BodyNode existing = model.getBody(planet.bodyId);
                    if (existing != null && existing.isPlanetary()) {
                        context.selectedPlanet = existing;
                    } else {
                        BodyNode provisional = new BodyNode(
                                planet.bodyId,
                                planet.bodyName,
                                BodyKind.PLANET,
                                planet.starSystem,
                                planet.systemAddress,
                                model.primaryStarId,
                                null,
                                null,
                                context.lastTimestamp
                        );
                        model.register(provisional);
                        context.selectedPlanet = provisional;
                    }
                });
            } else if ("Docked".equals(event)) {
                int bodyId = node.path("BodyID").asInt(-1);
                if (bodyId >= 0 && context.lastDockedStarSystem != null) {
                    SystemModel dockedModel = context.systems.get(key(context.lastDockedStarSystem));
                    if (dockedModel != null) {
                        BodyNode selected = dockedModel.getBody(bodyId);
                        if (selected != null && selected.isPlanetary()) {
                            context.selectedPlanet = selected;
                        }
                    }
                }
            } else if ("ScanOrganic".equals(event)) {
                long systemAddress = node.path("SystemAddress").asLong(0L);
                int bodyId = node.path("Body").asInt(-1);
                String scanType = textValue(node, "ScanType");
                String species = textValue(node, "Species");
                if (systemAddress > 0 && bodyId >= 0 && scanType != null) {
                    String bodyKey = organicBodyKey(systemAddress, bodyId);
                    context.lastOrganicScanTypeByBody.put(bodyKey, scanType);
                    if (species != null && !species.isBlank()) {
                        int idx = indexOfSpecies(species);
                        if (idx >= 0) {
                            OrganicCycleState state = context.lastOrganicCycleByBody.computeIfAbsent(
                                    bodyKey, k -> new OrganicCycleState()
                            );
                            switch (scanType.toLowerCase(Locale.ROOT)) {
                                case "sample" -> {
                                    state.speciesIndex = idx;
                                    state.nextStep = 2;
                                }
                                case "analyse", "analyze" -> {
                                    state.speciesIndex = (idx + 1) % ORGANIC_SPECIES_SEQUENCE.size();
                                    state.nextStep = 0;
                                }
                                default -> {
                                    state.speciesIndex = idx;
                                    state.nextStep = 1;
                                }
                            }
                        }
                    }
                }
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

        ObjectNode node = createBaseEvent("FSDJump", context);
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
        SystemModel model = context.systems.computeIfAbsent(
                key(starSystem),
                k -> new SystemModel(starSystem, systemAddress)
        );
        if (model.systemAddress <= 0) {
            model.systemAddress = systemAddress;
        }
        if (context.currentStarSystem != null && context.currentStarSystem.equalsIgnoreCase(starSystem) && context.currentSystemAddress > 0) {
            model.systemAddress = context.currentSystemAddress;
        }

        GeneratedScanBody generatedBody = generateScanBody(model, options);

        ObjectNode node = createBaseEvent("Scan", context);
        node.put("ScanType", generatedBody.scanType);
        node.put("BodyName", generatedBody.bodyName);
        node.put("BodyID", generatedBody.bodyId);

        ArrayNode parents = node.putArray("Parents");
        if (generatedBody.kind == BodyKind.STAR) {
            parents.addObject().put("Null", 0);
        } else if (generatedBody.kind == BodyKind.PLANET) {
            parents.addObject().put("Star", generatedBody.starParentId);
            parents.addObject().put("Null", 0);
        } else {
            parents.addObject().put("Planet", generatedBody.planetParentId);
            parents.addObject().put("Star", generatedBody.starParentId);
            parents.addObject().put("Null", 0);
        }

        node.put("StarSystem", model.starSystem);
        node.put("SystemAddress", model.systemAddress);
        node.put("DistanceFromArrivalLS", generatedBody.distanceFromArrivalLs);

        if (generatedBody.kind == BodyKind.STAR) {
            fillStarScanFields(node);
        } else {
            fillPlanetScanFields(node, generatedBody.kind == BodyKind.MOON);
        }
        model.register(generatedBody.toBodyNode(model.starSystem, model.systemAddress, context.lastTimestamp));
        return node;
    }

    private ObjectNode createScanOrganicEvent(JournalContext context, Map<String, String> options) {
        BodyNode selectedPlanet = resolveSelectedPlanet(context, options)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Aucune planete selectionnee: utilisez --selected-body-id ou scannez/selectionnez une planete avant ScanOrganic"
                ));

        long systemAddress = parseLongOption(options, "system-address").orElse(selectedPlanet.systemAddress);
        int bodyId = parseIntOption(options, "body-id").orElse(selectedPlanet.bodyId);

        OrganicCycleEvent organicCycle = nextOrganicCycleEvent(context, selectedPlanet);
        ObjectNode node = createBaseEvent("ScanOrganic", context);
        node.put("ScanType", options.getOrDefault("scan-type", organicCycle.scanType));
        OrganicSpeciesProfile profile = organicCycle.speciesProfile;
        node.put("Genus", profile.genusCodex);
        node.put("Genus_Localised", profile.genusLocalised);
        node.put("Species", profile.speciesCodex);
        node.put("Species_Localised", profile.speciesLocalised);
        node.put("Variant", profile.variantCodex);
        node.put("Variant_Localised", profile.variantLocalised);
        node.put("WasLogged", false);
        node.put("SystemAddress", systemAddress);
        node.put("Body", bodyId);
        return node;
    }

    private ObjectNode createDockedEvent(JournalContext context, Map<String, String> options) {
        BodyNode selectedPlanet = resolveSelectedPlanet(context, options)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Aucune planete selectionnee: utilisez --selected-body-id/--selected-body-name avant Docked"
                ));

        String starSystem = options.getOrDefault("system", selectedPlanet.starSystem);
        long systemAddress = parseLongOption(options, "system-address")
                .orElse(selectedPlanet.systemAddress);
        String defaultStation = selectedPlanet.bodyName + " Relay";

        ObjectNode node = createBaseEvent("Docked", context);
        node.put("StationName", options.getOrDefault("station-name", defaultStation));
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
        node.put("Body", selectedPlanet.bodyName);
        node.put("BodyID", selectedPlanet.bodyId);
        node.put("DistFromStarLS", selectedPlanet.distanceFromArrivalLs != null
                ? round(Math.max(1.0, selectedPlanet.distanceFromArrivalLs + randomInRange(-50.0, 120.0)), 3)
                : round(randomInRange(15.0, 1200.0), 3));
        return node;
    }

    private ObjectNode createFssBodySignalsEvent(JournalContext context, Map<String, String> options) {
        BodyNode selectedPlanet = resolveSelectedPlanet(context, options)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Aucune planete selectionnee: utilisez --selected-body-id/--selected-body-name avant FSSBodySignals"
                ));

        long systemAddress = parseLongOption(options, "system-address").orElse(selectedPlanet.systemAddress);
        int bodyId = parseIntOption(options, "body-id").orElse(selectedPlanet.bodyId);
        int signalCount = parseIntegerOptionInRange(options, "signal-count", 1, 8)
                .orElse(random.nextInt(1, 5));
        boolean geological = options.getOrDefault("signal-type", "biological")
                .equalsIgnoreCase("geological");

        ObjectNode node = createBaseEvent("FSSBodySignals", context);
        node.put("BodyName", selectedPlanet.bodyName);
        node.put("BodyID", bodyId);
        node.put("SystemAddress", systemAddress);
        ArrayNode signals = node.putArray("Signals");
        signals.add(buildSingleSignal(geological, signalCount));
        return node;
    }

    private ObjectNode createSaaSignalsFoundEvent(JournalContext context, Map<String, String> options) {
        BodyNode selectedPlanet = resolveSelectedPlanet(context, options)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Aucune planete selectionnee: utilisez --selected-body-id/--selected-body-name avant SAASignalsFound"
                ));

        long systemAddress = parseLongOption(options, "system-address").orElse(selectedPlanet.systemAddress);
        int bodyId = parseIntOption(options, "body-id").orElse(selectedPlanet.bodyId);
        int signalCount = parseIntegerOptionInRange(options, "signal-count", 1, 8)
                .orElse(random.nextInt(1, 5));
        boolean geological = options.getOrDefault("signal-type", "biological")
                .equalsIgnoreCase("geological");

        ObjectNode node = createBaseEvent("SAASignalsFound", context);
        node.put("BodyName", selectedPlanet.bodyName);
        node.put("SystemAddress", systemAddress);
        node.put("BodyID", bodyId);
        ArrayNode signals = node.putArray("Signals");
        signals.add(buildSingleSignal(geological, signalCount));
        if (!geological) {
            ArrayNode genuses = node.putArray("Genuses");
            for (OrganicSignature signature : pickOrganicSignatures(signalCount)) {
                ObjectNode genus = genuses.addObject();
                genus.put("Genus", signature.genusCodex);
                genus.put("Genus_Localised", signature.genusLocalised);
            }
        }
        return node;
    }

    private ObjectNode createBaseEvent(String eventName, JournalContext context) {
        ObjectNode node = OBJECT_MAPPER.createObjectNode();
        node.put("timestamp", nextTimestamp(context));
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
        SystemModel model = context.systems.get(key(starSystem));
        if (model != null && model.systemAddress > 0) {
            return model.systemAddress;
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

    private GeneratedScanBody generateScanBody(SystemModel model, Map<String, String> options) {
        Integer forcedBodyId = parseIntOption(options, "body-id").orElse(null);
        if (model.starCount() == 0) {
            return buildFirstStar(model, forcedBodyId);
        }

        ScanGenerationMode mode = pickScanMode(model);
        return switch (mode) {
            case ANOTHER_STAR -> buildAdditionalStar(model, forcedBodyId);
            case MOON_AROUND_PLANET -> buildMoonAroundPlanet(model, pickMoonParentPlanet(model), forcedBodyId);
            case MOON_FROM_EXISTING_MOON -> {
                BodyNode moonReference = pickExistingMoon(model);
                BodyNode parentPlanet = moonReference != null
                        ? model.getBody(moonReference.planetParentId)
                        : null;
                if (parentPlanet == null || parentPlanet.kind != BodyKind.PLANET) {
                    yield buildMoonAroundPlanet(model, pickMoonParentPlanet(model), forcedBodyId);
                }
                yield buildMoonAroundPlanet(model, parentPlanet, forcedBodyId);
            }
            case PLANET_AROUND_STAR, FIRST_STAR -> buildPlanetAroundStar(model, pickStarForPlanet(model), forcedBodyId);
        };
    }

    private ScanGenerationMode pickScanMode(SystemModel model) {
        List<WeightedMode> modes = new ArrayList<>();
        if (model.starCount() == 0) {
            return ScanGenerationMode.FIRST_STAR;
        }
        int planetCount = model.planetCount();
        int moonCount = model.moonCount();

        if (planetCount == 0) {
            modes.add(new WeightedMode(ScanGenerationMode.PLANET_AROUND_STAR, 78));
            modes.add(new WeightedMode(ScanGenerationMode.ANOTHER_STAR, 22));
        } else if (moonCount == 0) {
            modes.add(new WeightedMode(ScanGenerationMode.PLANET_AROUND_STAR, 52));
            modes.add(new WeightedMode(ScanGenerationMode.ANOTHER_STAR, 14));
            modes.add(new WeightedMode(ScanGenerationMode.MOON_AROUND_PLANET, 34));
        } else {
            modes.add(new WeightedMode(ScanGenerationMode.PLANET_AROUND_STAR, 48));
            modes.add(new WeightedMode(ScanGenerationMode.ANOTHER_STAR, 14));
            modes.add(new WeightedMode(ScanGenerationMode.MOON_AROUND_PLANET, 30));
            modes.add(new WeightedMode(ScanGenerationMode.MOON_FROM_EXISTING_MOON, 8));
        }

        int total = modes.stream().mapToInt(m -> m.weight).sum();
        int roll = random.nextInt(total);
        int cursor = 0;
        for (WeightedMode mode : modes) {
            cursor += mode.weight;
            if (roll < cursor) {
                return mode.mode;
            }
        }
        return modes.get(modes.size() - 1).mode;
    }

    private GeneratedScanBody buildFirstStar(SystemModel model, Integer forcedBodyId) {
        char starLetter = nextStarLetter(model);
        int bodyId = forcedBodyId != null ? forcedBodyId : model.nextBodyId();
        String bodyName = model.starSystem + " " + starLetter;
        return new GeneratedScanBody(
                bodyId,
                bodyName,
                BodyKind.STAR,
                null,
                null,
                "AutoScan",
                0.0
        );
    }

    private GeneratedScanBody buildAdditionalStar(SystemModel model, Integer forcedBodyId) {
        char starLetter = nextStarLetter(model);
        int bodyId = forcedBodyId != null ? forcedBodyId : model.nextBodyId();
        String bodyName = model.starSystem + " " + starLetter;
        return new GeneratedScanBody(
                bodyId,
                bodyName,
                BodyKind.STAR,
                null,
                null,
                "AutoScan",
                round(randomInRange(450.0, 160000.0), 6)
        );
    }

    private GeneratedScanBody buildPlanetAroundStar(SystemModel model, BodyNode starParent, Integer forcedBodyId) {
        if (starParent == null) {
            return buildAdditionalStar(model, forcedBodyId);
        }
        char starLetter = readStarLetter(model.starSystem, starParent.bodyName)
                .orElse('A');
        int planetIndex = nextPlanetIndexForStar(model, starParent.bodyId, starLetter);
        int bodyId = forcedBodyId != null ? forcedBodyId : model.nextBodyId();
        String bodyName = model.starSystem + " " + starLetter + " " + planetIndex;
        return new GeneratedScanBody(
                bodyId,
                bodyName,
                BodyKind.PLANET,
                starParent.bodyId,
                null,
                "Detailed",
                round(randomInRange(30.0, 4200.0), 6)
        );
    }

    private GeneratedScanBody buildMoonAroundPlanet(SystemModel model, BodyNode planetParent, Integer forcedBodyId) {
        if (planetParent == null) {
            return buildPlanetAroundStar(model, pickStarForPlanet(model), forcedBodyId);
        }
        Character moonLetter = nextMoonLetterForPlanet(model, planetParent.bodyId);
        if (moonLetter == null) {
            moonLetter = 'a';
        }
        int bodyId = forcedBodyId != null ? forcedBodyId : model.nextBodyId();
        Integer starParentId = planetParent.starParentId != null
                ? planetParent.starParentId
                : model.primaryStarId;
        String bodyName = planetParent.bodyName + " " + moonLetter;
        return new GeneratedScanBody(
                bodyId,
                bodyName,
                BodyKind.MOON,
                starParentId,
                planetParent.bodyId,
                "Detailed",
                round(Math.max(planetParent.distanceFromArrivalLs != null ? planetParent.distanceFromArrivalLs + randomInRange(0.2, 40.0) : randomInRange(45.0, 4400.0), 1.0), 6)
        );
    }

    private BodyNode pickStarForPlanet(SystemModel model) {
        List<BodyNode> stars = model.stars();
        if (stars.isEmpty()) {
            return null;
        }
        if (model.primaryStarId != null && random.nextDouble() < 0.72) {
            BodyNode primary = model.getBody(model.primaryStarId);
            if (primary != null) {
                return primary;
            }
        }
        return stars.get(random.nextInt(stars.size()));
    }

    private BodyNode pickMoonParentPlanet(SystemModel model) {
        List<BodyNode> planets = model.planets();
        if (planets.isEmpty()) {
            return null;
        }
        return planets.get(random.nextInt(planets.size()));
    }

    private BodyNode pickExistingMoon(SystemModel model) {
        List<BodyNode> moons = model.moons();
        if (moons.isEmpty()) {
            return null;
        }
        return moons.get(random.nextInt(moons.size()));
    }

    private char nextStarLetter(SystemModel model) {
        boolean[] used = new boolean[26];
        for (BodyNode star : model.stars()) {
            readStarLetter(model.starSystem, star.bodyName).ifPresent(letter -> {
                int index = letter - 'A';
                if (index >= 0 && index < used.length) {
                    used[index] = true;
                }
            });
        }
        for (int i = 0; i < used.length; i++) {
            if (!used[i]) {
                return (char) ('A' + i);
            }
        }
        return (char) ('A' + random.nextInt(26));
    }

    private int nextPlanetIndexForStar(SystemModel model, int starBodyId, char starLetter) {
        int max = 0;
        for (BodyNode planet : model.planets()) {
            if (planet.starParentId != null && planet.starParentId == starBodyId) {
                Optional<Integer> idx = readPlanetIndex(model.starSystem, planet.bodyName, starLetter);
                if (idx.isPresent()) {
                    max = Math.max(max, idx.get());
                }
            }
        }
        return Math.max(1, max + 1);
    }

    private Character nextMoonLetterForPlanet(SystemModel model, int planetBodyId) {
        char max = 0;
        for (BodyNode moon : model.moons()) {
            if (moon.planetParentId != null && moon.planetParentId == planetBodyId) {
                char letter = readMoonLetter(moon.bodyName).orElse((char) 0);
                if (letter > max) {
                    max = letter;
                }
            }
        }
        return max == 0 ? 'a' : (max == 'z' ? 'z' : (char) (max + 1));
    }

    private static Optional<Character> readStarLetter(String starSystem, String bodyName) {
        if (starSystem == null || bodyName == null) {
            return Optional.empty();
        }
        Pattern p = Pattern.compile("^" + Pattern.quote(starSystem) + " ([A-Z])$");
        Matcher m = p.matcher(bodyName);
        if (!m.matches()) {
            return Optional.empty();
        }
        return Optional.of(m.group(1).charAt(0));
    }

    private static Optional<Integer> readPlanetIndex(String starSystem, String bodyName, char starLetter) {
        if (starSystem == null || bodyName == null) {
            return Optional.empty();
        }
        Pattern p = Pattern.compile("^" + Pattern.quote(starSystem) + " " + starLetter + " (\\d+)$");
        Matcher m = p.matcher(bodyName);
        if (!m.matches()) {
            return Optional.empty();
        }
        return Optional.of(Integer.parseInt(m.group(1)));
    }

    private static Optional<Character> readMoonLetter(String bodyName) {
        if (bodyName == null) {
            return Optional.empty();
        }
        Pattern p = Pattern.compile("^.+ [A-Z] \\d+ ([a-z])$");
        Matcher m = p.matcher(bodyName);
        if (!m.matches()) {
            return Optional.empty();
        }
        return Optional.of(m.group(1).charAt(0));
    }

    private void fillStarScanFields(ObjectNode node) {
        String starType = STAR_TYPES.get(random.nextInt(STAR_TYPES.size()));
        node.put("StarType", starType);
        node.put("Subclass", random.nextInt(8));
        node.put("StellarMass", round(randomInRange(0.08, 2.5), 6));
        node.put("Radius", round(randomInRange(90_000_000.0, 690_000_000.0), 6));
        node.put("AbsoluteMagnitude", round(randomInRange(2.0, 14.0), 6));
        node.put("Age_MY", random.nextInt(13000));
        node.put("SurfaceTemperature", round(randomInRange(2400.0, 7700.0), 6));
        node.put("Luminosity", "V");
        node.put("SemiMajorAxis", randomInRange(1.8E10, 4.1E12));
        node.put("Eccentricity", round(randomInRange(0.0, 0.4), 6));
        node.put("OrbitalInclination", round(randomInRange(-80.0, 80.0), 6));
        node.put("Periapsis", round(randomInRange(0.0, 359.0), 6));
        node.put("OrbitalPeriod", randomInRange(1_200_000.0, 3_400_000_000.0));
        node.put("AscendingNode", round(randomInRange(-150.0, 150.0), 6));
        node.put("MeanAnomaly", round(randomInRange(0.0, 359.0), 6));
        node.put("RotationPeriod", randomInRange(42000.0, 420000.0));
        node.put("AxialTilt", 0.0);
        node.put("WasDiscovered", false);
        node.put("WasMapped", false);
        node.put("WasFootfalled", false);
    }

    private void fillPlanetScanFields(ObjectNode node, boolean moon) {
        node.put("TidalLock", random.nextBoolean());
        node.put("TerraformState", "");
        node.put("PlanetClass", moon ? "Rocky body" : "High metal content body");
        node.put("Atmosphere", "");
        node.put("AtmosphereType", "None");
        node.put("Volcanism", "");
        node.put("MassEM", round(randomInRange(0.001, 0.008), 6));
        node.put("Radius", round(randomInRange(650000.0, 1450000.0), 6));
        node.put("SurfaceGravity", round(randomInRange(0.85, 2.25), 6));
        node.put("SurfaceTemperature", round(randomInRange(80.0, 650.0), 6));
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
    }

    private OrganicCycleEvent nextOrganicCycleEvent(JournalContext context, BodyNode selectedPlanet) {
        String key = organicBodyKey(selectedPlanet.systemAddress, selectedPlanet.bodyId);
        OrganicCycleState state = context.lastOrganicCycleByBody.computeIfAbsent(key, unused -> new OrganicCycleState());
        int speciesCount = ORGANIC_SPECIES_SEQUENCE.size();
        if (speciesCount == 0) {
            throw new IllegalStateException("Aucune espece organique configuree");
        }
        int speciesIndex = Math.floorMod(state.speciesIndex, speciesCount);
        OrganicSpeciesProfile profile = ORGANIC_SPECIES_SEQUENCE.get(speciesIndex);
        String scanType = switch (Math.floorMod(state.nextStep, 3)) {
            case 0, 1 -> "Sample";
            default -> "Analyse";
        };

        state.nextStep++;
        if (state.nextStep >= 3) {
            state.nextStep = 0;
            state.speciesIndex = (speciesIndex + 1) % speciesCount;
        } else {
            state.speciesIndex = speciesIndex;
        }
        return new OrganicCycleEvent(scanType, profile);
    }

    private Optional<BodyNode> resolveSelectedPlanet(JournalContext context, Map<String, String> options) {
        Optional<Integer> selectedBodyIdOpt = parseIntOption(options, "selected-body-id");
        if (selectedBodyIdOpt.isPresent()) {
            int bodyId = selectedBodyIdOpt.get();
            Optional<BodyNode> byId = findBodyById(context, bodyId);
            if (byId.isPresent() && byId.get().isPlanetary()) {
                return byId;
            }
        }

        String selectedBodyName = options.get("selected-body-name");
        if (selectedBodyName != null && !selectedBodyName.isBlank()) {
            Optional<BodyNode> byName = findBodyByName(context, selectedBodyName.trim());
            if (byName.isPresent() && byName.get().isPlanetary()) {
                return byName;
            }
        }

        if (context.selectedPlanet != null) {
            return Optional.of(context.selectedPlanet);
        }

        if (context.currentStarSystem != null) {
            SystemModel model = context.systems.get(key(context.currentStarSystem));
            if (model != null) {
                return model.latestPlanetaryBody();
            }
        }
        return context.findAnyPlanetaryBody();
    }

    private Optional<BodyNode> findBodyById(JournalContext context, int bodyId) {
        if (context.currentStarSystem != null) {
            SystemModel current = context.systems.get(key(context.currentStarSystem));
            if (current != null) {
                BodyNode body = current.getBody(bodyId);
                if (body != null) {
                    return Optional.of(body);
                }
            }
        }
        for (SystemModel model : context.systems.values()) {
            BodyNode body = model.getBody(bodyId);
            if (body != null) {
                return Optional.of(body);
            }
        }
        return Optional.empty();
    }

    private Optional<BodyNode> findBodyByName(JournalContext context, String bodyName) {
        if (context.currentStarSystem != null) {
            SystemModel current = context.systems.get(key(context.currentStarSystem));
            if (current != null) {
                Optional<BodyNode> found = current.findBodyByName(bodyName);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        for (SystemModel model : context.systems.values()) {
            Optional<BodyNode> found = model.findBodyByName(bodyName);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
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

    private static Optional<Integer> parseIntegerOptionInRange(
            Map<String, String> options,
            String key,
            int minInclusive,
            int maxInclusive
    ) {
        Optional<Integer> value = parseIntOption(options, key);
        if (value.isEmpty()) {
            return Optional.empty();
        }
        int parsed = value.get();
        if (parsed < minInclusive || parsed > maxInclusive) {
            throw new IllegalArgumentException(
                    "Option hors borne --" + key + "=" + parsed
                            + " (attendu: " + minInclusive + ".." + maxInclusive + ")"
            );
        }
        return Optional.of(parsed);
    }

    private ObjectNode buildSingleSignal(boolean geological, int count) {
        ObjectNode signal = OBJECT_MAPPER.createObjectNode();
        if (geological) {
            signal.put("Type", "$SAA_SignalType_Geological;");
            signal.put("Type_Localised", "Geologique");
        } else {
            signal.put("Type", "$SAA_SignalType_Biological;");
            signal.put("Type_Localised", "Biologique");
        }
        signal.put("Count", Math.max(1, count));
        return signal;
    }

    private List<OrganicSignature> pickOrganicSignatures(int signalCount) {
        if (signalCount <= 0) {
            return List.of();
        }
        int count = Math.min(signalCount, ORGANIC_SIGNATURE_POOL.size());
        int start = random.nextInt(ORGANIC_SIGNATURE_POOL.size());
        List<OrganicSignature> signatures = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            signatures.add(ORGANIC_SIGNATURE_POOL.get((start + i) % ORGANIC_SIGNATURE_POOL.size()));
        }
        return signatures;
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

    private static String organicBodyKey(long systemAddress, int bodyId) {
        return systemAddress + ":" + bodyId;
    }

    private static Optional<BodyNode> buildBodyNodeFromScan(JsonNode node, Instant timestamp) {
        int bodyId = node.path("BodyID").asInt(-1);
        String bodyName = textValue(node, "BodyName");
        String starSystem = textValue(node, "StarSystem");
        long systemAddress = node.path("SystemAddress").asLong(0L);
        if (bodyId < 0 || bodyName == null || starSystem == null || starSystem.isBlank()) {
            return Optional.empty();
        }

        Integer starParentId = null;
        Integer planetParentId = null;
        JsonNode parents = node.path("Parents");
        if (parents.isArray()) {
            for (JsonNode parent : parents) {
                if (parent.has("Star") && starParentId == null) {
                    starParentId = parent.path("Star").asInt();
                } else if (parent.has("Planet") && planetParentId == null) {
                    planetParentId = parent.path("Planet").asInt();
                }
            }
        }

        BodyKind kind;
        if (node.has("StarType")) {
            kind = BodyKind.STAR;
        } else if (node.has("PlanetClass")) {
            kind = planetParentId != null ? BodyKind.MOON : BodyKind.PLANET;
        } else {
            return Optional.empty();
        }

        Double distance = node.has("DistanceFromArrivalLS")
                ? node.path("DistanceFromArrivalLS").asDouble()
                : null;

        return Optional.of(new BodyNode(
                bodyId,
                bodyName,
                kind,
                starSystem,
                systemAddress,
                starParentId,
                planetParentId,
                distance,
                timestamp
        ));
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
                  mvn -pl elite-journal-simulator exec:java -Dexec.mainClass=be.mirooz.elitedangerous.dashboard.tools.ExplorationJournalEventSimulatorTool -Dexec.args="<event> [--journal-dir=PATH] [--key=value]"

                Events supportes:
                  fsdjump
                  scan
                  scanorganic
                  docked
                  fssbodysignals
                  saasignalsfound

                Exemples:
                  ... -Dexec.args="fsdjump --journal-dir=elite-journal-simulator/src/main/resources/exemple"
                  ... -Dexec.args="scan --scan-type=Detailed"
                  ... -Dexec.args="scanorganic --selected-body-id=12"
                  ... -Dexec.args="docked --selected-body-name=Swoilz RE-U c18-3 B 1"
                  ... -Dexec.args="fssbodysignals --selected-body-id=12 --signal-type=biological"
                  ... -Dexec.args="saasignalsfound --selected-body-id=12 --signal-count=2"
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
            candidates.add(Path.of("elite-journal-simulator/src/main/resources/exemple"));
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
        private BodyNode selectedPlanet;
        private Instant lastTimestamp;
        private final Map<String, PlanetContext> lastPlanetBySystem = new HashMap<>();
        private final Map<String, Integer> maxBodyIdBySystem = new HashMap<>();
        private final Map<String, SystemModel> systems = new HashMap<>();
        private final Map<String, String> lastOrganicScanTypeByBody = new HashMap<>();
        private final Map<String, OrganicCycleState> lastOrganicCycleByBody = new HashMap<>();

        private Optional<BodyNode> findAnyPlanetaryBody() {
            for (SystemModel model : systems.values()) {
                Optional<BodyNode> body = model.latestPlanetaryBody();
                if (body.isPresent()) {
                    return body;
                }
            }
            return Optional.empty();
        }
    }

    private record PlanetContext(String bodyName, int bodyId, String starSystem, long systemAddress) {
    }

    private enum BodyKind {
        STAR,
        PLANET,
        MOON
    }

    private enum ScanGenerationMode {
        FIRST_STAR,
        PLANET_AROUND_STAR,
        ANOTHER_STAR,
        MOON_AROUND_PLANET,
        MOON_FROM_EXISTING_MOON
    }

    private record WeightedMode(ScanGenerationMode mode, int weight) {
    }

    private static final class BodyNode {
        private final int bodyId;
        private final String bodyName;
        private final BodyKind kind;
        private final String starSystem;
        private final long systemAddress;
        private final Integer starParentId;
        private final Integer planetParentId;
        private final Double distanceFromArrivalLs;
        private final Instant timestamp;

        private BodyNode(int bodyId,
                         String bodyName,
                         BodyKind kind,
                         String starSystem,
                         long systemAddress,
                         Integer starParentId,
                         Integer planetParentId,
                         Double distanceFromArrivalLs,
                         Instant timestamp) {
            this.bodyId = bodyId;
            this.bodyName = bodyName;
            this.kind = kind;
            this.starSystem = starSystem;
            this.systemAddress = systemAddress;
            this.starParentId = starParentId;
            this.planetParentId = planetParentId;
            this.distanceFromArrivalLs = distanceFromArrivalLs;
            this.timestamp = timestamp;
        }

        private boolean isPlanetary() {
            return kind == BodyKind.PLANET || kind == BodyKind.MOON;
        }
    }

    private static final class OrganicCycleState {
        private int speciesIndex;
        private int nextStep;
    }

    private record OrganicSpeciesProfile(
            String genusCodex,
            String genusLocalised,
            String speciesCodex,
            String speciesLocalised,
            String variantCodex,
            String variantLocalised
    ) {
    }

    private record OrganicSignature(
            String genusCodex,
            String genusLocalised
    ) {
    }

    private static final class SystemModel {
        private final String starSystem;
        private long systemAddress;
        private int maxBodyId;
        private Integer primaryStarId;
        private final Map<Integer, BodyNode> bodiesById = new HashMap<>();

        private SystemModel(String starSystem, long systemAddress) {
            this.starSystem = starSystem;
            this.systemAddress = systemAddress;
        }

        private void register(BodyNode body) {
            bodiesById.put(body.bodyId, body);
            maxBodyId = Math.max(maxBodyId, body.bodyId);
            if (body.kind == BodyKind.STAR) {
                if (primaryStarId == null) {
                    primaryStarId = body.bodyId;
                } else if (readStarLetter(starSystem, body.bodyName).orElse('Z') == 'A') {
                    primaryStarId = body.bodyId;
                }
            }
        }

        private int nextBodyId() {
            return Math.max(1, maxBodyId + 1);
        }

        private int starCount() {
            return (int) bodiesById.values().stream().filter(b -> b.kind == BodyKind.STAR).count();
        }

        private int planetCount() {
            return (int) bodiesById.values().stream().filter(b -> b.kind == BodyKind.PLANET).count();
        }

        private int moonCount() {
            return (int) bodiesById.values().stream().filter(b -> b.kind == BodyKind.MOON).count();
        }

        private List<BodyNode> stars() {
            return bodiesById.values().stream()
                    .filter(b -> b.kind == BodyKind.STAR)
                    .sorted(Comparator.comparingInt(b -> b.bodyId))
                    .toList();
        }

        private List<BodyNode> planets() {
            return bodiesById.values().stream()
                    .filter(b -> b.kind == BodyKind.PLANET)
                    .sorted(Comparator.comparingInt(b -> b.bodyId))
                    .toList();
        }

        private List<BodyNode> moons() {
            return bodiesById.values().stream()
                    .filter(b -> b.kind == BodyKind.MOON)
                    .sorted(Comparator.comparingInt(b -> b.bodyId))
                    .toList();
        }

        private BodyNode getBody(Integer bodyId) {
            if (bodyId == null) {
                return null;
            }
            return bodiesById.get(bodyId);
        }

        private Optional<BodyNode> latestPlanetaryBody() {
            return bodiesById.values().stream()
                    .filter(BodyNode::isPlanetary)
                    .max(Comparator.comparing(b -> b.timestamp != null ? b.timestamp : Instant.EPOCH));
        }

        private Optional<BodyNode> findBodyByName(String bodyName) {
            return bodiesById.values().stream()
                    .filter(b -> b.bodyName.equalsIgnoreCase(bodyName))
                    .findFirst();
        }
    }

    private record GeneratedScanBody(
            int bodyId,
            String bodyName,
            BodyKind kind,
            Integer starParentId,
            Integer planetParentId,
            String scanType,
            double distanceFromArrivalLs
    ) {
        BodyNode toBodyNode(String starSystem, long systemAddress, Instant timestamp) {
            return new BodyNode(
                    bodyId,
                    bodyName,
                    kind,
                    starSystem,
                    systemAddress,
                    starParentId,
                    planetParentId,
                    distanceFromArrivalLs,
                    timestamp
            );
        }
    }
}
