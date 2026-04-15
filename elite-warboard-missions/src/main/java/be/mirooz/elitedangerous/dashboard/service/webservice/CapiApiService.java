package be.mirooz.elitedangerous.dashboard.service.webservice;

import be.mirooz.elitedangerous.capi.client.relay.EddnCommodityMessageFactory;
import be.mirooz.elitedangerous.capi.client.relay.InaraEventFactory;
import be.mirooz.elitedangerous.capi.client.relay.MarketRelayCommander;
import be.mirooz.elitedangerous.capi.client.relay.CapiClient;
import be.mirooz.elitedangerous.dashboard.model.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.view.common.context.DashboardContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

/**
 * Orchestration marché : CAPI /market, EDDN commodity/3, Inara (côté dashboard uniquement).
 */
public final class CapiApiService {

    private static final CapiApiService INSTANCE = new CapiApiService();

    private final CapiClient relay = CapiClient.getInstance();

    private CapiApiService() {
    }

    public static CapiApiService getInstance() {
        return INSTANCE;
    }

    public void sendMarketDatas(JsonNode journalMarketEvent) {
        try {
            if (!DashboardContext.getInstance().isBatchLoading()) {
                JsonNode capiMarket = relay.getMarket();

                long journalMid = parseLongNode(journalMarketEvent.path("MarketID"));
                long capiMid = parseLongNode(capiMarket.path("id"));
                if (journalMid != 0L && capiMid != 0L && journalMid != capiMid) {
                    System.err.println("EDDN: MarketID journal (" + journalMid + ") != CAPI (" + capiMid + "), envoi annulé.");
                    return;
                }
                CommanderStatus s = CommanderStatus.getInstance();
                MarketRelayCommander cmd = new MarketRelayCommander(
                        s.getFID(),
                        s.getCommanderName(),
                        s.getCurrentStarSystem(),
                        s.getCurrentStationName(),
                        s.getGameVersion(),
                        s.getGameBuild(),
                        s.getHorizons(),
                        s.getOdyssey());
                ObjectNode envelope = EddnCommodityMessageFactory.build(journalMarketEvent, capiMarket, cmd);
                JsonNode message = envelope.path("message");
                sendToEddn(message, envelope);
                //sendToInara(message, cmd);
            }

        } catch (Exception e) {
            System.err.println("Warboard relay (market): " + e.getMessage());
        }
    }

    private void sendToEddn(JsonNode message, ObjectNode envelope) throws IOException {
        if (message.path("marketId").asLong(0L) == 0L) {
            System.err.println("EDDN: marketId manquant, envoi annulé.");
            return;
        }
        if (message.path("systemName").asText("").isBlank()
                || message.path("stationName").asText("").isBlank()
                || message.path("timestamp").asText("").isBlank()) {
            System.err.println("EDDN: systemName/stationName/timestamp manquants, envoi annulé.");
            return;
        }
        if (message.path("commodities").isArray() && message.path("commodities").isEmpty()) {
            System.err.println("EDDN: aucune commodity CAPI exploitable, envoi annulé.");
            return;
        }

        relay.sendEddnCommodity(envelope);
        System.out.println("EDDN: marché envoyé (" + relay.eddnRelayUrl() + ")");
    }

    private void sendToInara(JsonNode message, MarketRelayCommander cmd) throws IOException {
        String inaraApiKey = InaraEventFactory.resolveInaraApiKey();
        if (inaraApiKey.isBlank()) {
            System.err.println("INARA: API key manquante (ELITE_INARA_API_KEY), envoi ignoré.");
            return;
        }
        ObjectNode inaraPayload = InaraEventFactory.buildDockFromMarketEvent(
                message.path("timestamp").asText(""),
                message.path("systemName").asText(""),
                message.path("stationName").asText(""),
                message.path("marketId").asLong(0L),
                cmd);
        relay.sendInaraBatch(inaraPayload);
        System.out.println("INARA: event addCommanderTravelDock envoyé (" + relay.inaraRelayUrl() + ")");
    }

    private static long parseLongNode(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return 0L;
        }
        if (node.isIntegralNumber()) {
            return node.longValue();
        }
        if (node.isTextual()) {
            try {
                return Long.parseLong(node.asText().trim());
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        return 0L;
    }
}
