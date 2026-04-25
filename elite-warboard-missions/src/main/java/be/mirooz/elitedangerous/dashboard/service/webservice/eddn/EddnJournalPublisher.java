package be.mirooz.elitedangerous.dashboard.service.webservice.eddn;

import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.service.PreferencesService;
import be.mirooz.elitedangerous.eddn.EddnSchemas;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Routeur EDDN : invoqué par chaque {@code JournalEventHandler} via le décorateur
 * {@link be.mirooz.elitedangerous.dashboard.handlers.events.EddnPublishingEventHandlerDecorator},
 * il détermine le schéma cible à partir du champ {@code event}, délègue la conversion à un
 * {@link EddnEventMappers mapper typé} qui renvoie un POJO EDDN généré, puis publie via
 * {@link EddnUploader}.
 *
 * <p>La classe ne manipule plus aucun {@link com.fasterxml.jackson.databind.node.ObjectNode} :
 * toute la logique de mapping / enrichissement / transformation spec vit dans
 * {@link EddnEventMappers}. Le flux est donc :</p>
 * <pre>
 *   JsonNode raw  →  mapper.mapXxx(raw)  →  EddnMessages.Xxx POJO  →  uploader.publishMessage(...)
 * </pre>
 *
 * <p>Le routeur conserve une seule responsabilité annexe : tracker le contexte commandant
 * ({@code SystemAddress}, {@code StarPos}) sur les events navigationnels, pour que les mappers
 * puissent enrichir les events qui ne les contiennent pas nativement.</p>
 */
public final class EddnJournalPublisher {

    private static final EddnJournalPublisher INSTANCE = new EddnJournalPublisher();

    private final EddnUploader uploader = EddnUploader.getInstance();
    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();
    private final PreferencesService preferencesService = PreferencesService.getInstance();
    private final EddnEventMappers mappers = new EddnEventMappers(commanderStatus);

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
        if (!preferencesService.isSendDataToEddnEnabled()) {
            return;
        }
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

    private void route(String event, JsonNode raw) throws Exception {
        if (EddnEventMappers.JOURNAL_SCHEMA_EVENTS.contains(event)) {
            send(EddnSchemas.JOURNAL_V1, mappers.mapJournal(raw));
            return;
        }
        switch (event) {
            case "ApproachSettlement":
                send(EddnSchemas.APPROACH_SETTLEMENT_V1, mappers.mapApproachSettlement(raw));
                break;
            case "CodexEntry":
                send(EddnSchemas.CODEX_ENTRY_V1, mappers.mapCodexEntry(raw));
                break;
            case "DockingDenied":
                send(EddnSchemas.DOCKING_DENIED_V1, mappers.mapDockingDenied(raw));
                break;
            case "DockingGranted":
                send(EddnSchemas.DOCKING_GRANTED_V1, mappers.mapDockingGranted(raw));
                break;
            case "FCMaterials":
                send(EddnSchemas.FC_MATERIALS_JOURNAL_V1, mappers.mapFcMaterialsJournal(raw));
                break;
            case "FSSAllBodiesFound":
                send(EddnSchemas.FSS_ALL_BODIES_FOUND_V1, mappers.mapFssAllBodiesFound(raw));
                break;
            case "FSSBodySignals":
                send(EddnSchemas.FSS_BODY_SIGNALS_V1, mappers.mapFssBodySignals(raw));
                break;
            case "FSSDiscoveryScan":
                send(EddnSchemas.FSS_DISCOVERY_SCAN_V1, mappers.mapFssDiscoveryScan(raw));
                break;
            case "FSSSignalDiscovered":
                send(EddnSchemas.FSS_SIGNAL_DISCOVERED_V1, mappers.mapFssSignalDiscovered(raw));
                break;
            case "NavBeaconScan":
                send(EddnSchemas.NAV_BEACON_SCAN_V1, mappers.mapNavBeaconScan(raw));
                break;
            case "ScanBaryCentre":
                send(EddnSchemas.SCAN_BARY_CENTRE_V1, mappers.mapScanBaryCentre(raw));
                break;

            // Schémas lus depuis les fichiers compagnons du jeu.
            case "Market":
                send(EddnSchemas.COMMODITY_V3, mappers.mapCommodity(EddnJournalFileReader.readMarket(), raw));
                break;
            case "Outfitting":
                send(EddnSchemas.OUTFITTING_V2, mappers.mapOutfitting(EddnJournalFileReader.readOutfitting(), raw));
                break;
            case "Shipyard":
                send(EddnSchemas.SHIPYARD_V2, mappers.mapShipyard(EddnJournalFileReader.readShipyard(), raw));
                break;
            case "NavRoute":
                send(EddnSchemas.NAV_ROUTE_V1, mappers.mapNavRoute(EddnJournalFileReader.readNavRoute(), raw));
                break;
            default:
                // Event non relayé à EDDN.
                break;
        }
    }

    /** Délègue à l'uploader en filtrant les POJOs null (mapper a décidé qu'il n'y avait rien à publier). */
    private void send(String schemaRef, Object pojo) {
        if (pojo == null) {
            return;
        }
        uploader.publishMessage(schemaRef, pojo);
    }

    // ------------------------------------------------------------------
    //  Suivi du contexte commandant : alimente CommanderStatus pour que les mappers puissent
    //  enrichir les events qui ne contiennent pas nativement StarPos / SystemAddress.
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
}
