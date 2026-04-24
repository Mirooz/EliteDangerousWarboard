package be.mirooz.elitedangerous.dashboard.service.webservice.eddn;

import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderStatus;
import be.mirooz.elitedangerous.eddn.EddnSchemas;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Set;

/**
 * Point d'entrée unique de publication EDDN : invoqué par chaque {@code JournalEventHandler}
 * dont l'événement est relayé à EDDN. Détermine le schéma cible, enrichit le message avec le
 * contexte commandant, puis délègue à {@link EddnUploader} qui filtre les champs personnels
 * et envoie effectivement au gateway.
 *
 * <p>Les événements {@code Docked}, {@code FSDJump}, {@code Location}, {@code CarrierJump},
 * {@code Scan}, {@code SAASignalsFound} vont sous le schéma générique {@code journal/v1}.
 * Les autres événements ont chacun leur schéma dédié.</p>
 *
 * <p>{@code NavRoute}, {@code Market}, {@code Outfitting}, {@code Shipyard} ne contiennent pas
 * la donnée utile dans l'event : on déclenche une lecture du fichier compagnon
 * ({@link EddnJournalFileReader}).</p>
 */
public final class EddnJournalPublisher {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Events routés vers le schéma journal générique. */
    private static final Set<String> JOURNAL_SCHEMA_EVENTS = Set.of(
            "Docked", "FSDJump", "Location", "CarrierJump", "Scan", "SAASignalsFound"
    );

    private static final EddnJournalPublisher INSTANCE = new EddnJournalPublisher();

    private final EddnUploader uploader = EddnUploader.getInstance();
    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();

    private EddnJournalPublisher() {
    }

    public static EddnJournalPublisher getInstance() {
        return INSTANCE;
    }

    /**
     * Point d'entrée unique : à appeler depuis un {@code JournalEventHandler} une fois son traitement
     * métier effectué. Ne lève jamais, ne bloque jamais l'appelant.
     */
    public void publish(JsonNode jsonNode) {
        if (jsonNode == null || !jsonNode.isObject()) {
            return;
        }
        String event = jsonNode.path("event").asText(null);
        if (event == null) {
            return;
        }

        trackCommanderContext(event, jsonNode);

        try {
            route(event, jsonNode);
        } catch (Exception e) {
            System.err.println("EDDN route " + event + " : " + e.getMessage());
        }
    }

    private void route(String event, JsonNode raw) {
        if (JOURNAL_SCHEMA_EVENTS.contains(event)) {
            publishJournalSchema(raw);
            return;
        }
        switch (event) {
            case "ApproachSettlement":
                publishEnriched(EddnSchemas.APPROACH_SETTLEMENT_V1, raw, true, true);
                break;
            case "CodexEntry":
                publishEnriched(EddnSchemas.CODEX_ENTRY_V1, raw, true, true);
                break;
            case "DockingDenied":
                // Schéma n'accepte PAS StarSystem ni StarPos : uniquement station / market / reason.
                publishEnriched(EddnSchemas.DOCKING_DENIED_V1, raw, false, false);
                break;
            case "DockingGranted":
                // Schéma n'accepte PAS StarSystem ni StarPos : uniquement station / market / pad.
                publishEnriched(EddnSchemas.DOCKING_GRANTED_V1, raw, false, false);
                break;
            case "FCMaterials":
                publishEnriched(EddnSchemas.FC_MATERIALS_JOURNAL_V1, raw, false, false);
                break;
            case "FSSAllBodiesFound":
                publishEnriched(EddnSchemas.FSS_ALL_BODIES_FOUND_V1, raw, true, true);
                break;
            case "FSSBodySignals":
                publishEnriched(EddnSchemas.FSS_BODY_SIGNALS_V1, raw, true, true);
                break;
            case "FSSDiscoveryScan":
                publishEnriched(EddnSchemas.FSS_DISCOVERY_SCAN_V1, raw, true, true);
                break;
            case "FSSSignalDiscovered":
                publishFssSignalDiscovered(raw);
                break;
            case "NavBeaconScan":
                publishEnriched(EddnSchemas.NAV_BEACON_SCAN_V1, raw, true, true);
                break;
            case "ScanBaryCentre":
                publishEnriched(EddnSchemas.SCAN_BARY_CENTRE_V1, raw, true, true);
                break;

            // Schémas lus depuis les fichiers compagnons du jeu.
            case "Market":
                publishCommodityFromFile(raw);
                break;
            case "Outfitting":
                publishOutfittingFromFile(raw);
                break;
            case "Shipyard":
                publishShipyardFromFile(raw);
                break;
            case "NavRoute":
                publishNavRouteFromFile(raw);
                break;
            default:
                // Event non relayé à EDDN.
                break;
        }
    }

    // ------------------------------------------------------------------
    //  Suivi du contexte commandant (StarPos / SystemAddress) nécessaire
    //  pour enrichir les events qui ne les contiennent pas.
    // ------------------------------------------------------------------

    private void trackCommanderContext(String event, JsonNode raw) {
        boolean navigational = "FSDJump".equals(event)
                || "Location".equals(event)
                || "CarrierJump".equals(event);
        if (!navigational) {
            return;
        }
        if (raw.has("SystemAddress") && raw.get("SystemAddress").canConvertToLong()) {
            commanderStatus.setCurrentSystemAddress(raw.get("SystemAddress").asLong());
        }
        JsonNode pos = raw.get("StarPos");
        if (pos != null && pos.isArray() && pos.size() == 3) {
            double[] xyz = new double[] {
                    pos.get(0).asDouble(),
                    pos.get(1).asDouble(),
                    pos.get(2).asDouble()
            };
            commanderStatus.setCurrentStarPos(xyz);
        }
    }

    // ------------------------------------------------------------------
    //  Schéma journal générique
    // ------------------------------------------------------------------

    private void publishJournalSchema(JsonNode raw) {
        ObjectNode msg = (ObjectNode) raw.deepCopy();
        ensureStarSystem(msg);
        ensureStarPosAndSystemAddress(msg);
        ensureHorizonsOdyssey(msg);
        uploader.publishMessage(EddnSchemas.JOURNAL_V1, msg);
    }

    // ------------------------------------------------------------------
    //  Schémas dédiés
    // ------------------------------------------------------------------

    /**
     * @param needsStarSystem        injecte {@code StarSystem} si absent
     * @param needsStarPosAndSysAddr injecte {@code StarPos} et {@code SystemAddress} si absents
     */
    private void publishEnriched(String schemaRef,
                                 JsonNode raw,
                                 boolean needsStarSystem,
                                 boolean needsStarPosAndSysAddr) {
        ObjectNode msg = (ObjectNode) raw.deepCopy();
        if (needsStarSystem) {
            ensureStarSystem(msg);
        }
        if (needsStarPosAndSysAddr) {
            ensureStarPosAndSystemAddress(msg);
        }
        ensureHorizonsOdyssey(msg);
        uploader.publishMessage(schemaRef, msg);
    }

    /**
     * Champs systémiques du message EDDN {@code fsssignaldiscovered/1}. Tout le reste
     * (SignalName, SignalType, IsStation, USSType, ThreatLevel, SpawningFaction, ...) doit
     * être déplacé dans l'élément du tableau {@code signals[]}.
     */
    private static final Set<String> FSS_SIGNAL_TOP_LEVEL_FIELDS = Set.of(
            "timestamp", "event", "SystemAddress", "StarSystem", "StarPos", "horizons", "odyssey"
    );

    /**
     * Transforme l'event journal {@code FSSSignalDiscovered} (un seul signal à plat) en message
     * EDDN conforme au schéma {@code fsssignaldiscovered/1} qui exige un tableau {@code signals[]}
     * et interdit les champs {@code SignalName / SignalType / IsStation / ...} au top-level.
     */
    private void publishFssSignalDiscovered(JsonNode raw) {
        if (!raw.has("SignalName")) {
            return; // schéma : SignalName est requis dans l'item
        }
        ObjectNode msg = MAPPER.createObjectNode();
        msg.put("timestamp", raw.path("timestamp").asText());
        msg.put("event", "FSSSignalDiscovered");
        if (raw.has("SystemAddress")) {
            msg.set("SystemAddress", raw.get("SystemAddress"));
        }

        ObjectNode signal = MAPPER.createObjectNode();
        raw.fields().forEachRemaining(e -> {
            if (!FSS_SIGNAL_TOP_LEVEL_FIELDS.contains(e.getKey())) {
                signal.set(e.getKey(), e.getValue());
            }
        });
        if (!signal.has("timestamp")) {
            signal.put("timestamp", raw.path("timestamp").asText());
        }

        ArrayNode signals = MAPPER.createArrayNode();
        signals.add(signal);
        msg.set("signals", signals);

        ensureStarSystem(msg);
        ensureStarPosAndSystemAddress(msg);
        ensureHorizonsOdyssey(msg);
        uploader.publishMessage(EddnSchemas.FSS_SIGNAL_DISCOVERED_V1, msg);
    }

    // ------------------------------------------------------------------
    //  Schémas basés sur les fichiers compagnons
    // ------------------------------------------------------------------

    private void publishCommodityFromFile(JsonNode raw) {
        JsonNode file = EddnJournalFileReader.readMarket();
        if (file == null || !file.isObject()) {
            return;
        }
        long eventMarketId = raw.path("MarketID").asLong(-1);
        long fileMarketId = file.path("MarketID").asLong(-2);
        if (eventMarketId > 0 && fileMarketId > 0 && eventMarketId != fileMarketId) {
            return; // fichier pas encore à jour pour ce marché
        }
        ObjectNode msg = MAPPER.createObjectNode();
        msg.put("timestamp", file.path("timestamp").asText(raw.path("timestamp").asText()));
        msg.put("systemName", file.path("StarSystem").asText(commanderStatus.getCurrentStarSystem()));
        msg.put("stationName", file.path("StationName").asText(commanderStatus.getCurrentStationName()));
        msg.put("marketId", fileMarketId > 0 ? fileMarketId : eventMarketId);
        JsonNode carrierDockingAccess = file.path("CarrierDockingAccess");
        if (!carrierDockingAccess.isMissingNode()) {
            msg.set("carrierDockingAccess", carrierDockingAccess);
        }

        ArrayNode filtered = MAPPER.createArrayNode();
        JsonNode commodities = file.path("Items");
        if (!commodities.isArray() || commodities.isEmpty()) {
            return;
        }
        for (JsonNode c : commodities) {
            if (shouldSkipCommodity(c)) {
                continue;
            }
            filtered.add(buildCommodityEntry(c));
        }
        if (filtered.isEmpty()) {
            return;
        }
        msg.set("commodities", filtered);
        // Augmentations EDDN (cf. README-EDDN-schemas : horizons/odyssey flags).
        ensureHorizonsOdyssey(msg);
        uploader.publishMessage(EddnSchemas.COMMODITY_V3, msg);
    }

    /**
     * Règles d'élision EDDN (commodity/3) pour Market.json :
     * <ul>
     *   <li>Skip si {@code Category}/{@code categoryname} contient {@code NonMarketable} (limpets, etc.).</li>
     *   <li>Skip si {@code Legality} est renseigné (commodity pas normalement tradée à ce marché).</li>
     *   <li>Skip si aucun {@code Name}.</li>
     * </ul>
     */
    private boolean shouldSkipCommodity(JsonNode c) {
        if (c == null || !c.isObject()) {
            return true;
        }
        String category = c.path("Category").asText("");
        if (category.isEmpty()) {
            category = c.path("categoryname").asText(""); // variante CAPI
        }
        if (category.toLowerCase().contains("nonmarketable")) {
            return true;
        }
        String legality = c.path("Legality").asText("");
        if (!legality.isBlank()) {
            return true;
        }
        return !c.has("Name");
    }

    private ObjectNode buildCommodityEntry(JsonNode src) {
        ObjectNode out = MAPPER.createObjectNode();
        out.put("name", cleanCommoditySymbol(src.path("Name").asText()));
        out.put("meanPrice", src.path("MeanPrice").asInt(0));
        out.put("buyPrice", src.path("BuyPrice").asInt(0));
        out.put("sellPrice", src.path("SellPrice").asInt(0));
        out.put("stock", src.path("Stock").asInt(0));
        out.put("demand", src.path("Demand").asInt(0));
        out.put("stockBracket", src.path("StockBracket").asInt(0));
        out.put("demandBracket", src.path("DemandBracket").asInt(0));
        JsonNode statusFlags = src.path("StatusFlags");
        if (statusFlags.isArray() && !statusFlags.isEmpty()) {
            out.set("statusFlags", statusFlags);
        }
        return out;
    }

    /**
     * Nettoie le symbole commodity tel que retourné par {@code Market.json} / journal :
     * <pre>
     *   "$gold_name;"        -> "gold"
     *   "$HydrogenFuel_Name;"-> "hydrogenfuel"
     *   "Gold"               -> "gold"   (déjà clean)
     * </pre>
     * EDDN (schéma commodity/3) attend un symbole minuscule sans le wrapper {@code $..._name;}.
     */
    private static String cleanCommoditySymbol(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim().toLowerCase();
        if (s.startsWith("$")) {
            s = s.substring(1);
        }
        if (s.endsWith(";")) {
            s = s.substring(0, s.length() - 1);
        }
        if (s.endsWith("_name")) {
            s = s.substring(0, s.length() - "_name".length());
        }
        return s;
    }

    private void publishOutfittingFromFile(JsonNode raw) {
        JsonNode file = EddnJournalFileReader.readOutfitting();
        if (file == null || !file.isObject()) {
            return;
        }
        long eventMarketId = raw.path("MarketID").asLong(-1);
        long fileMarketId = file.path("MarketID").asLong(-2);
        if (eventMarketId > 0 && fileMarketId > 0 && eventMarketId != fileMarketId) {
            return;
        }
        ObjectNode msg = MAPPER.createObjectNode();
        msg.put("timestamp", file.path("timestamp").asText(raw.path("timestamp").asText()));
        msg.put("systemName", file.path("StarSystem").asText(commanderStatus.getCurrentStarSystem()));
        msg.put("stationName", file.path("StationName").asText(commanderStatus.getCurrentStationName()));
        msg.put("marketId", fileMarketId > 0 ? fileMarketId : eventMarketId);
        msg.put("horizons", Boolean.TRUE.equals(commanderStatus.getHorizons()));
        msg.put("odyssey", Boolean.TRUE.equals(commanderStatus.getOdyssey()));

        ArrayNode modules = MAPPER.createArrayNode();
        JsonNode items = file.path("Items");
        if (!items.isArray() || items.isEmpty()) {
            return;
        }
        for (JsonNode m : items) {
            String name = m.path("Name").asText("");
            if (name.isEmpty() || isNonShipModule(name)) {
                continue;
            }
            modules.add(name.toLowerCase());
        }
        if (modules.isEmpty()) {
            return;
        }
        msg.set("modules", modules);
        uploader.publishMessage(EddnSchemas.OUTFITTING_V2, msg);
    }

    /** EDDN outfitting exclut les modules SRV / fighter / prototypes. Filtre minimal, peu intrusif. */
    private boolean isNonShipModule(String name) {
        String n = name.toLowerCase();
        return n.contains("_fighter_")
                || n.contains("testbuggy")
                || n.startsWith("hpt_cargomissilepod");
    }

    private void publishShipyardFromFile(JsonNode raw) {
        JsonNode file = EddnJournalFileReader.readShipyard();
        if (file == null || !file.isObject()) {
            return;
        }
        long eventMarketId = raw.path("MarketID").asLong(-1);
        long fileMarketId = file.path("MarketID").asLong(-2);
        if (eventMarketId > 0 && fileMarketId > 0 && eventMarketId != fileMarketId) {
            return;
        }
        ObjectNode msg = MAPPER.createObjectNode();
        msg.put("timestamp", file.path("timestamp").asText(raw.path("timestamp").asText()));
        msg.put("systemName", file.path("StarSystem").asText(commanderStatus.getCurrentStarSystem()));
        msg.put("stationName", file.path("StationName").asText(commanderStatus.getCurrentStationName()));
        msg.put("marketId", fileMarketId > 0 ? fileMarketId : eventMarketId);
        msg.put("horizons", Boolean.TRUE.equals(commanderStatus.getHorizons()));
        msg.put("odyssey", Boolean.TRUE.equals(commanderStatus.getOdyssey()));

        ArrayNode ships = MAPPER.createArrayNode();
        JsonNode priceList = file.path("PriceList");
        if (!priceList.isArray() || priceList.isEmpty()) {
            return;
        }
        for (JsonNode s : priceList) {
            String shipType = s.path("ShipType").asText("");
            if (shipType.isEmpty()) {
                continue;
            }
            ships.add(shipType.toLowerCase());
        }
        if (ships.isEmpty()) {
            return;
        }
        msg.set("ships", ships);
        uploader.publishMessage(EddnSchemas.SHIPYARD_V2, msg);
    }

    private void publishNavRouteFromFile(JsonNode raw) {
        JsonNode file = EddnJournalFileReader.readNavRoute();
        if (file == null || !file.isObject()) {
            return;
        }
        ObjectNode msg = (ObjectNode) file.deepCopy();
        // Le fichier est déjà conforme au schéma (timestamp + event + Route[]).
        // On injecte horizons/odyssey au besoin, utiles au consommateur.
        if (!msg.has("timestamp")) {
            msg.put("timestamp", raw.path("timestamp").asText());
        }
        uploader.publishMessage(EddnSchemas.NAV_ROUTE_V1, msg);
    }

    // ------------------------------------------------------------------
    //  Helpers enrichissement
    // ------------------------------------------------------------------

    private void ensureStarSystem(ObjectNode msg) {
        if (msg.has("StarSystem") && !msg.path("StarSystem").asText("").isEmpty()) {
            return;
        }
        String system = commanderStatus.getCurrentStarSystem();
        if (system != null && !system.isBlank()) {
            msg.put("StarSystem", system);
        }
    }

    private void ensureHorizonsOdyssey(ObjectNode msg) {
        if (!msg.has("horizons")) {
            Boolean horizons = commanderStatus.getHorizons();
            if (horizons != null) {
                msg.put("horizons", horizons);
            }
        }
        if (!msg.has("odyssey")) {
            Boolean odyssey = commanderStatus.getOdyssey();
            if (odyssey != null) {
                msg.put("odyssey", odyssey);
            }
        }
    }

    private void ensureStarPosAndSystemAddress(ObjectNode msg) {
        if (!msg.has("SystemAddress")) {
            Long sysAddr = commanderStatus.getCurrentSystemAddress();
            if (sysAddr != null) {
                msg.put("SystemAddress", sysAddr);
            }
        }
        if (!msg.has("StarPos")) {
            double[] pos = commanderStatus.getCurrentStarPos();
            if (pos != null && pos.length == 3) {
                ArrayNode arr = MAPPER.createArrayNode();
                arr.add(pos[0]);
                arr.add(pos[1]);
                arr.add(pos[2]);
                msg.set("StarPos", arr);
            }
        }
    }

}
