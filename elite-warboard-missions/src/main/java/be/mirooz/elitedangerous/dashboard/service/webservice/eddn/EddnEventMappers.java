package be.mirooz.elitedangerous.dashboard.service.webservice.eddn;

import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderStatus;
import be.mirooz.elitedangerous.eddn.EddnEnvelope;
import be.mirooz.elitedangerous.eddn.EddnMessages;
import be.mirooz.elitedangerous.eddn.generated.EddnMessage__10;
import be.mirooz.elitedangerous.eddn.generated.EddnMessage__3;
import be.mirooz.elitedangerous.eddn.generated.EddnMessage__4;
import be.mirooz.elitedangerous.eddn.generated.EddnSignal__1;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Mappers : {@code journal JsonNode} → {@code EDDN POJO typé} ({@link EddnMessages}).
 *
 * <p>Un mapper par schéma EDDN. Chaque mapper renvoie un POJO typé généré à partir des JSON Schemas
 * officiels, garantissant par construction que l'output respecte {@code additionalProperties: false}
 * pour les schémas stricts (les setters et la {@code @JsonPropertyOrder} héritée du POJO généré
 * définissent exactement les champs sérialisables).</p>
 *
 * <p>Deux techniques cohabitent, selon la complexité :</p>
 * <ul>
 *   <li><b>Field-by-field</b> : lecture explicite des champs du raw event et appel des setters du POJO.
 *       Utilisé pour {@code DockingGranted}, {@code DockingDenied}, {@code FSSSignalDiscovered} où le
 *       reshaping (signals[] en particulier) est trop spécifique pour un mapping automatique.</li>
 *   <li><b>Enrich + {@link ObjectMapper#treeToValue treeToValue}</b> : l'{@link ObjectNode} enrichi
 *       (StarSystem / StarPos / horizons / odyssey injectés au besoin) est converti en POJO par Jackson.
 *       Les champs inconnus du POJO sont silencieusement droppés ({@code FAIL_ON_UNKNOWN_PROPERTIES=false}
 *       configuré sur {@link EddnEnvelope#mapper()}). Utilisé pour les schémas strict-mais-riches
 *       (ApproachSettlement, CodexEntry, FSS*, NavBeaconScan, ScanBaryCentre, FCMaterials) et pour le
 *       {@code journal/1} lax (la POJO Journal expose un {@code Map<String,Object> additionalProperties}
 *       qui préserve les champs event-spécifiques).</li>
 * </ul>
 *
 * <p>Les mappers <b>fichier-compagnon</b> ({@link #mapCommodity}, {@link #mapOutfitting},
 * {@link #mapShipyard}, {@link #mapNavRoute}) prennent en entrée le {@link JsonNode} du fichier déjà lu
 * — leur extraction est déléguée à {@link EddnJournalFileReader}.</p>
 */
final class EddnEventMappers {

    private static final ObjectMapper MAPPER = EddnEnvelope.mapper();

    /** Valeur {@code USSType} dont le signal ne doit JAMAIS être publié (spec EDDN : PII). */
    private static final String USS_TYPE_MISSION_TARGET = "$USS_Type_MissionTarget;";

    private final CommanderStatus commanderStatus;

    EddnEventMappers(CommanderStatus commanderStatus) {
        this.commanderStatus = commanderStatus;
    }

    // ==================================================================
    //  Schéma journal/1 (lax) : 6 events, additionalProperties géré par le POJO
    // ==================================================================

    /**
     * Mappe un event {@code Docked / FSDJump / Location / CarrierJump / Scan / SAASignalsFound}
     * vers {@link EddnMessages.Journal}. Les champs event-spécifiques (ex. {@code BodyName},
     * {@code StationType}, {@code Signals}…) finissent dans {@code additionalProperties} du POJO
     * et sont re-sérialisés intacts.
     */
    EddnMessages.Journal mapJournal(JsonNode raw) throws JsonProcessingException {
        ObjectNode msg = (ObjectNode) raw.deepCopy();
        // Spec journal-README : sur Location, Latitude/Longitude sont des données personnelles à exclure.
        // (Note : pour ApproachSettlement, au contraire, elles DOIVENT être présentes — autre schéma.)
        if ("Location".equals(msg.path("event").asText(""))) {
            msg.remove("Latitude");
            msg.remove("Longitude");
        }
        ensureStarSystem(msg);
        ensureStarPosAndSystemAddress(msg);
        ensureHorizonsOdyssey(msg);
        return MAPPER.treeToValue(msg, EddnMessages.Journal.class);
    }

    // ==================================================================
    //  Schémas stricts, field-by-field
    // ==================================================================

    /**
     * {@code dockinggranted/1} : schéma strict refusant {@code StarSystem} / {@code StarPos}.
     * Construction explicite des seuls champs autorisés.
     */
    EddnMessages.DockingGranted mapDockingGranted(JsonNode raw) {
        EddnMessages.DockingGranted msg = new EddnMessages.DockingGranted();
        msg.setTimestamp(raw.path("timestamp").asText());
        msg.setEvent(EddnMessage__4.Event.DOCKING_GRANTED);
        if (raw.has("MarketID")) {
            msg.setMarketID(raw.get("MarketID").asLong());
        }
        msg.setStationName(textOrNull(raw, "StationName"));
        msg.setStationType(textOrNull(raw, "StationType"));
        if (raw.has("LandingPad")) {
            msg.setLandingPad(raw.get("LandingPad").asLong());
        }
        msg.setHorizons(commanderStatus.getHorizons());
        msg.setOdyssey(commanderStatus.getOdyssey());
        return msg;
    }

    /** {@code dockingdenied/1} : idem {@link #mapDockingGranted}, avec {@code Reason} en plus. */
    EddnMessages.DockingDenied mapDockingDenied(JsonNode raw) {
        EddnMessages.DockingDenied msg = new EddnMessages.DockingDenied();
        msg.setTimestamp(raw.path("timestamp").asText());
        msg.setEvent(EddnMessage__3.Event.DOCKING_DENIED);
        if (raw.has("MarketID")) {
            msg.setMarketID(raw.get("MarketID").asLong());
        }
        msg.setStationName(textOrNull(raw, "StationName"));
        msg.setStationType(textOrNull(raw, "StationType"));
        msg.setReason(textOrNull(raw, "Reason"));
        msg.setHorizons(commanderStatus.getHorizons());
        msg.setOdyssey(commanderStatus.getOdyssey());
        return msg;
    }

    /**
     * {@code fsssignaldiscovered/1} : le journal émet <i>un</i> signal à plat ; EDDN attend un
     * tableau {@code signals[]} avec les champs signal ({@code SignalName}, {@code SignalType}, …)
     * nichés dans chaque élément. Renvoie {@code null} si l'event doit être droppé (signal invalide
     * ou {@code USSType == $USS_Type_MissionTarget;}).
     */
    EddnMessages.FSSSignalDiscovered mapFssSignalDiscovered(JsonNode raw) {
        if (!raw.has("SignalName")) {
            return null;
        }
        if (USS_TYPE_MISSION_TARGET.equals(raw.path("USSType").asText(""))) {
            return null;
        }
        EddnMessages.FSSSignalDiscovered msg = new EddnMessages.FSSSignalDiscovered();
        msg.setTimestamp(raw.path("timestamp").asText());
        msg.setEvent(EddnMessage__10.Event.FSS_SIGNAL_DISCOVERED);
        if (raw.has("SystemAddress")) {
            msg.setSystemAddress(raw.get("SystemAddress").asLong());
        }
        fillStarSystemSetter(msg::setStarSystem, raw);
        fillStarPosSetter(msg::setStarPos, raw);
        msg.setHorizons(commanderStatus.getHorizons());
        msg.setOdyssey(commanderStatus.getOdyssey());

        EddnSignal__1 signal = new EddnSignal__1();
        signal.setTimestamp(raw.path("timestamp").asText());
        signal.setSignalName(raw.path("SignalName").asText());
        if (raw.has("SignalType")) {
            signal.setSignalType(raw.get("SignalType").asText());
        }
        if (raw.has("IsStation")) {
            signal.setIsStation(raw.get("IsStation").asBoolean());
        }
        if (raw.has("USSType")) {
            signal.setUSSType(raw.get("USSType").asText());
        }
        if (raw.has("SpawningState")) {
            signal.setSpawningState(raw.get("SpawningState").asText());
        }
        if (raw.has("SpawningFaction")) {
            signal.setSpawningFaction(raw.get("SpawningFaction").asText());
        }
        if (raw.has("SpawningPower")) {
            signal.setSpawningPower(raw.get("SpawningPower").asText());
        }
        if (raw.has("OpposingPower")) {
            signal.setOpposingPower(raw.get("OpposingPower").asText());
        }
        if (raw.has("ThreatLevel") && raw.get("ThreatLevel").canConvertToLong()) {
            signal.setThreatLevel(raw.get("ThreatLevel").asLong());
        }
        // TimeRemaining volontairement omis (spec EDDN : éphémère, donnée PII).

        List<EddnSignal__1> signals = new ArrayList<>(1);
        signals.add(signal);
        msg.setSignals(signals);
        return msg;
    }

    // ==================================================================
    //  Schémas stricts-mais-riches : enrichissement + treeToValue
    // ==================================================================

    EddnMessages.ApproachSettlement mapApproachSettlement(JsonNode raw) throws JsonProcessingException {
        return enrichAndConvert(raw, EddnMessages.ApproachSettlement.class, true, true);
    }

    EddnMessages.CodexEntry mapCodexEntry(JsonNode raw) throws JsonProcessingException {
        return enrichAndConvert(raw, EddnMessages.CodexEntry.class, true, true);
    }

    EddnMessages.FCMaterialsJournal mapFcMaterialsJournal(JsonNode raw) throws JsonProcessingException {
        return enrichAndConvert(raw, EddnMessages.FCMaterialsJournal.class, false, false);
    }

    EddnMessages.FSSAllBodiesFound mapFssAllBodiesFound(JsonNode raw) throws JsonProcessingException {
        return enrichAndConvert(raw, EddnMessages.FSSAllBodiesFound.class, true, true);
    }

    EddnMessages.FSSBodySignals mapFssBodySignals(JsonNode raw) throws JsonProcessingException {
        return enrichAndConvert(raw, EddnMessages.FSSBodySignals.class, true, true);
    }

    EddnMessages.FSSDiscoveryScan mapFssDiscoveryScan(JsonNode raw) throws JsonProcessingException {
        return enrichAndConvert(raw, EddnMessages.FSSDiscoveryScan.class, true, true);
    }

    EddnMessages.NavBeaconScan mapNavBeaconScan(JsonNode raw) throws JsonProcessingException {
        return enrichAndConvert(raw, EddnMessages.NavBeaconScan.class, true, true);
    }

    EddnMessages.ScanBaryCentre mapScanBaryCentre(JsonNode raw) throws JsonProcessingException {
        return enrichAndConvert(raw, EddnMessages.ScanBaryCentre.class, true, true);
    }

    // ==================================================================
    //  Schémas fichiers compagnons (Market.json / Outfitting.json / …)
    // ==================================================================

    /**
     * {@code commodity/3} : lecture du {@code Market.json}, filtrage (non-tradeable, illégal,
     * limpets) et nettoyage des symboles ({@code "$gold_name;"} → {@code "gold"}). Renvoie
     * {@code null} si le marché n'a aucune commodity publiable.
     *
     * <p>Note : la définition {@code levelType} du schéma ({@code enum: [0, 1, 2, 3, ""]}) est
     * patchée en {@code {"type": "integer"}} au build (cf. {@code elite-eddn-client/pom.xml},
     * étape {@code fetch-eddn-schemas}) pour que le POJO généré expose un {@code Long} au lieu
     * d'un enum String qui sérialiserait {@code "3"} au lieu de {@code 3}.</p>
     */
    EddnMessages.Commodity mapCommodity(JsonNode file, JsonNode raw) throws JsonProcessingException {
        if (file == null || !file.isObject()) {
            return null;
        }
        if (!matchingMarketIds(raw, file)) {
            return null;
        }
        long fileMarketId = file.path("MarketID").asLong(-1);
        JsonNode commodities = file.path("Items");
        if (!commodities.isArray() || commodities.isEmpty()) {
            return null;
        }

        ObjectNode msg = MAPPER.createObjectNode();
        msg.put("timestamp", file.path("timestamp").asText(raw.path("timestamp").asText()));
        msg.put("systemName", file.path("StarSystem").asText(commanderStatus.getCurrentStarSystem()));
        msg.put("stationName", file.path("StationName").asText(commanderStatus.getCurrentStationName()));
        msg.put("marketId", fileMarketId);
        JsonNode carrierDockingAccess = file.path("CarrierDockingAccess");
        if (!carrierDockingAccess.isMissingNode()) {
            msg.set("carrierDockingAccess", carrierDockingAccess);
        }

        ArrayNode filtered = MAPPER.createArrayNode();
        for (JsonNode c : commodities) {
            if (shouldSkipCommodity(c)) {
                continue;
            }
            filtered.add(buildCommodityEntry(c));
        }
        if (filtered.isEmpty()) {
            return null;
        }
        msg.set("commodities", filtered);
        ensureHorizonsOdyssey(msg);
        return MAPPER.treeToValue(msg, EddnMessages.Commodity.class);
    }

    /**
     * {@code outfitting/2} : whitelist stricte des modules autorisés par la spec
     * ({@code Hpt_*}, {@code Int_*}, {@code *_Armour_*}) — les cosmétiques sont droppés.
     */
    EddnMessages.Outfitting mapOutfitting(JsonNode file, JsonNode raw) throws JsonProcessingException {
        if (file == null || !file.isObject()) {
            return null;
        }
        if (!matchingMarketIds(raw, file)) {
            return null;
        }
        long fileMarketId = file.path("MarketID").asLong(-1);
        JsonNode items = file.path("Items");
        if (!items.isArray() || items.isEmpty()) {
            return null;
        }
        ArrayNode modules = MAPPER.createArrayNode();
        for (JsonNode m : items) {
            String name = m.path("Name").asText("");
            if (name.isEmpty() || !isEddnOutfittingModule(name)) {
                continue;
            }
            modules.add(name.toLowerCase());
        }
        if (modules.isEmpty()) {
            return null;
        }
        ObjectNode msg = MAPPER.createObjectNode();
        msg.put("timestamp", file.path("timestamp").asText(raw.path("timestamp").asText()));
        msg.put("systemName", file.path("StarSystem").asText(commanderStatus.getCurrentStarSystem()));
        msg.put("stationName", file.path("StationName").asText(commanderStatus.getCurrentStationName()));
        msg.put("marketId", fileMarketId);
        msg.put("horizons", Boolean.TRUE.equals(commanderStatus.getHorizons()));
        msg.put("odyssey", Boolean.TRUE.equals(commanderStatus.getOdyssey()));
        msg.set("modules", modules);
        return MAPPER.treeToValue(msg, EddnMessages.Outfitting.class);
    }

    /** {@code shipyard/2} : liste des {@code ShipType} disponibles au marché, en minuscules. */
    EddnMessages.Shipyard mapShipyard(JsonNode file, JsonNode raw) throws JsonProcessingException {
        if (file == null || !file.isObject()) {
            return null;
        }
        if (!matchingMarketIds(raw, file)) {
            return null;
        }
        long fileMarketId = file.path("MarketID").asLong(-1);
        JsonNode priceList = file.path("PriceList");
        if (!priceList.isArray() || priceList.isEmpty()) {
            return null;
        }
        ArrayNode ships = MAPPER.createArrayNode();
        for (JsonNode s : priceList) {
            String shipType = s.path("ShipType").asText("");
            if (shipType.isEmpty()) {
                continue;
            }
            ships.add(shipType.toLowerCase());
        }
        if (ships.isEmpty()) {
            return null;
        }
        ObjectNode msg = MAPPER.createObjectNode();
        msg.put("timestamp", file.path("timestamp").asText(raw.path("timestamp").asText()));
        msg.put("systemName", file.path("StarSystem").asText(commanderStatus.getCurrentStarSystem()));
        msg.put("stationName", file.path("StationName").asText(commanderStatus.getCurrentStationName()));
        msg.put("marketId", fileMarketId);
        msg.put("horizons", Boolean.TRUE.equals(commanderStatus.getHorizons()));
        msg.put("odyssey", Boolean.TRUE.equals(commanderStatus.getOdyssey()));
        msg.set("ships", ships);
        return MAPPER.treeToValue(msg, EddnMessages.Shipyard.class);
    }

    /**
     * {@code navroute/1} : le fichier {@code NavRoute.json} est déjà conforme au schéma
     * ({@code timestamp} + {@code event} + {@code Route[]}), on se contente de compléter
     * le timestamp et les flags horizons/odyssey.
     */
    /**
     * {@code navroute/1} : le fichier {@code NavRoute.json} est réécrit à chaque modification,
     * y compris les <b>effacements</b> ({@code event == "NavRouteClear"}). EDDN n'accepte que
     * l'event {@code NavRoute} avec un {@code Route[]} non vide — on skip sinon.
     */
    EddnMessages.NavRoute mapNavRoute(JsonNode file, JsonNode raw) throws JsonProcessingException {
        if (file == null || !file.isObject()) {
            return null;
        }
        // NavRouteClear / timestamp seul / event autre → rien à publier.
        if (!"NavRoute".equals(file.path("event").asText(""))) {
            return null;
        }
        JsonNode route = file.path("Route");
        if (!route.isArray() || route.isEmpty()) {
            return null;
        }
        ObjectNode msg = (ObjectNode) file.deepCopy();
        if (!msg.has("timestamp")) {
            msg.put("timestamp", raw.path("timestamp").asText());
        }
        ensureHorizonsOdyssey(msg);
        return MAPPER.treeToValue(msg, EddnMessages.NavRoute.class);
    }

    // ==================================================================
    //  Helpers
    // ==================================================================

    /**
     * Vérifie que le {@code MarketID} de l'event journal et celui du fichier compagnon
     * (Market.json / Outfitting.json / Shipyard.json) sont bien présents ET identiques.
     *
     * <p>Si les deux divergent, c'est que le fichier compagnon n'a pas encore été rafraîchi
     * par le jeu pour le marché du dernier event — publier dans cet état enverrait des données
     * obsolètes (commodities d'un autre marché) à EDDN. On refuse donc la publication.</p>
     *
     * <p>Un {@code MarketID} absent (ou non-numérique) est également un signal d'incohérence :
     * on refuse aussi — mieux vaut un event perdu qu'une donnée fausse côté EDDN.</p>
     */
    private static boolean matchingMarketIds(JsonNode raw, JsonNode file) {
        long eventMarketId = raw.path("MarketID").asLong(-1);
        long fileMarketId = file.path("MarketID").asLong(-2);
        return eventMarketId > 0 && fileMarketId > 0 && eventMarketId == fileMarketId;
    }

    private <T> T enrichAndConvert(JsonNode raw,
                                   Class<T> pojoClass,
                                   boolean needsStarSystem,
                                   boolean needsStarPosAndSysAddr) throws JsonProcessingException {
        ObjectNode msg = (ObjectNode) raw.deepCopy();
        if (needsStarSystem) {
            ensureStarSystem(msg);
        }
        if (needsStarPosAndSysAddr) {
            ensureStarPosAndSystemAddress(msg);
        }
        ensureHorizonsOdyssey(msg);
        return MAPPER.treeToValue(msg, pojoClass);
    }

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

    /**
     * Résout le {@code StarSystem} (raw event → commandant) pour les mappers field-by-field.
     * Abstrait le setter afin que l'appelant n'ait pas à dupliquer la logique de fallback.
     */
    private void fillStarSystemSetter(java.util.function.Consumer<String> setter, JsonNode raw) {
        String fromRaw = raw.path("StarSystem").asText("");
        if (!fromRaw.isBlank()) {
            setter.accept(fromRaw);
            return;
        }
        String fromCtx = commanderStatus.getCurrentStarSystem();
        if (fromCtx != null && !fromCtx.isBlank()) {
            setter.accept(fromCtx);
        }
    }

    /**
     * Résout {@code StarPos} (raw event → commandant) pour les mappers field-by-field.
     * Le setter attend une {@code List<Double>} (format du POJO).
     */
    private void fillStarPosSetter(java.util.function.Consumer<List<Double>> setter, JsonNode raw) {
        JsonNode pos = raw.path("StarPos");
        if (pos.isArray() && pos.size() == 3) {
            setter.accept(List.of(pos.get(0).asDouble(), pos.get(1).asDouble(), pos.get(2).asDouble()));
            return;
        }
        double[] ctx = commanderStatus.getCurrentStarPos();
        if (ctx != null && ctx.length == 3) {
            setter.accept(List.of(ctx[0], ctx[1], ctx[2]));
        }
    }

    private static String textOrNull(JsonNode raw, String field) {
        JsonNode n = raw.path(field);
        return n.isMissingNode() || n.isNull() ? null : n.asText(null);
    }

    /** Commodities à ignorer à la publication EDDN (spec {@code commodity/3}). */
    private static boolean shouldSkipCommodity(JsonNode c) {
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

    /**
     * Construit une entry {@code commodities[]} conforme à {@code commodity/3} (lowercase + symbole
     * nettoyé : {@code "$gold_name;"} → {@code "gold"}).
     */
    private static ObjectNode buildCommodityEntry(JsonNode src) {
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
     * Nettoie le symbole commodity ({@code "$gold_name;"} / {@code "$HydrogenFuel_Name;"} /
     * {@code "Gold"}) vers le format attendu par {@code commodity/3} : minuscule, sans
     * le wrapper {@code $..._name;}.
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

    /**
     * Spec {@code outfitting-README} : whitelist stricte des modules publiables. Cosmétiques
     * (bobbleheads, decals, paintjobs, shipkits…) explicitement rejetés par le schéma.
     * Exclusion historique : {@code Int_PlanetApproachSuite}.
     */
    private static boolean isEddnOutfittingModule(String name) {
        String n = name.toLowerCase();
        if (n.equals("int_planetapproachsuite")) {
            return false;
        }
        return n.startsWith("hpt_") || n.startsWith("int_") || n.contains("_armour_");
    }

    /** Evénements du schéma journal/1 (lax). Exposé pour le routeur. */
    static final Set<String> JOURNAL_SCHEMA_EVENTS = Set.of(
            "Docked", "FSDJump", "Location", "CarrierJump", "Scan", "SAASignalsFound"
    );
}
