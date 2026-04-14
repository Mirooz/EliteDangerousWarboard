package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.capi.client.FrontierCapiClient;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * Service applicatif pour accéder au Frontier CAPI.
 */
public class CapiService {

    private static CapiService instance;

    private final FrontierCapiClient capiClient;

    private CapiService() {
        this.capiClient = new FrontierCapiClient();
    }

    public static synchronized CapiService getInstance() {
        if (instance == null) {
            instance = new CapiService();
        }
        return instance;
    }

    public JsonNode getCommanderProfile(String accessToken) throws IOException {
        return capiClient.getProfile(accessToken);
    }

    public JsonNode getFleetCarrierInfo(String accessToken) throws IOException {
        return capiClient.getFleetCarrier(accessToken);
    }

    public JsonNode getFleetCarrierCargo(String accessToken) throws IOException {
        JsonNode fleetCarrier = getFleetCarrierInfo(accessToken);
        return fleetCarrier.path("cargo");
    }

    public JsonNode getFleetCarrierMarketOrders(String accessToken) throws IOException {
        JsonNode fleetCarrier = getFleetCarrierInfo(accessToken);
        return fleetCarrier.path("orders").path("commodities");
    }

    public JsonNode getEndpoint(String endpoint, String accessToken) throws IOException {
        return capiClient.getJson(endpoint, accessToken);
    }
}
