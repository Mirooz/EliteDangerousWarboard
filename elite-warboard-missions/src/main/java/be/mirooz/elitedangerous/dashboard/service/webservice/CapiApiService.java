package be.mirooz.elitedangerous.dashboard.service.webservice;

import be.mirooz.elitedangerous.capi.client.relay.CapiClient;
import be.mirooz.elitedangerous.dashboard.model.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.view.common.context.DashboardContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Orchestration marché : CAPI /market, EDDN commodity/3, Inara (côté dashboard uniquement).
 */
public final class CapiApiService {

    private static final CapiApiService INSTANCE = new CapiApiService();

    private final CapiClient capiClient = CapiClient.getInstance();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private CapiApiService() {
    }

    public static CapiApiService getInstance() {
        return INSTANCE;
    }

    public void sendMarketDatas(JsonNode journalMarketEvent) {
        try {
            if (!DashboardContext.getInstance().isBatchLoading()) {
                CommanderStatus s = CommanderStatus.getInstance();
                ObjectNode payload = objectMapper.createObjectNode();
                payload.put("fid", s.getFID());
                payload.put("commanderName", s.getCommanderName());
                payload.set("event", journalMarketEvent.deepCopy());

                capiClient.sendMarket(payload);
            }

        } catch (Exception e) {
            System.err.println("Warboard relay (market): " + e.getMessage());
        }
    }
}
