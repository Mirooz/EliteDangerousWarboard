package be.mirooz.elitedangerous.dashboard.service.webservice;

import be.mirooz.elitedangerous.capi.client.CapiClient;
import be.mirooz.elitedangerous.capi.client.UnauthorizedException;
import be.mirooz.elitedangerous.capi.generated.model.CapiApiResponse;
import be.mirooz.elitedangerous.capi.generated.model.CapiMarketEvent;
import be.mirooz.elitedangerous.capi.generated.model.CapiMarketProxyRequest;
import be.mirooz.elitedangerous.dashboard.model.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.view.common.context.DashboardContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

public final class CapiApiService {

    private static final CapiApiService INSTANCE = new CapiApiService();

    private final CapiClient capiClient = CapiClient.getInstance();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private CapiApiService() {}

    public static CapiApiService getInstance() {
        return INSTANCE;
    }

    public void sendMarketDatas(JsonNode journalMarketEvent) {
        if (DashboardContext.getInstance().isBatchLoading()) {
            return;
        }
        try {
            CommanderStatus s = CommanderStatus.getInstance();
            CapiMarketEvent event = objectMapper.convertValue(
                    journalMarketEvent,
                    CapiMarketEvent.class
            );
            CapiMarketProxyRequest payload = new CapiMarketProxyRequest()
                    .fid(s.getFID())
                    .commanderName(s.getCommanderName())
                    .event(event);
            CapiApiResponse response = capiClient.postMarket(payload);
            handleSuccess(response);
        } catch (UnauthorizedException e) {
            handleFrontierAuth(e.getResponse());
        } catch (IOException e) {
            System.err.println("Market send error: " + e.getMessage());
        }
    }

    // =========================
    // SUCCESS
    // =========================

    private void handleSuccess(CapiApiResponse r) {
        if (r == null) {
            System.err.println("CAPI market: réponse vide");
            return;
        }
        if ("success".equalsIgnoreCase(r.getStatus())) {
            System.out.println("EDDN: marché envoyé via CAPI");
            return;
        }

        System.err.println("CAPI market: réponse inattendue " + r);
    }

    // =========================
    // AUTH
    // =========================

    private void handleFrontierAuth(CapiApiResponse response) {
        if (response == null || response.getError() == null) {
            System.err.println("CAPI market: réponse auth vide");
            return;
        }

        Map<String, Object> details = response.getError().getDetails();
        if (details == null) {
            System.err.println("CAPI market: détails auth absents");
            return;
        }

        String action = (String) details.get("action");

        if ("authenticate_frontier".equalsIgnoreCase(action)) {

            String loginUrl = (String) details.get("loginUrl");
            String errorMessage = response.getError().getMessage();

            if (errorMessage != null && !errorMessage.isBlank()) {
                System.err.println("CAPI market: " + errorMessage);
            }

            openBrowser(loginUrl);
            return;
        }

        System.err.println("CAPI market: réponse auth inattendue " + response);
    }

    // =========================
    // BROWSER
    // =========================

    private void openBrowser(String loginUrl) {
        if (loginUrl == null || loginUrl.isBlank()) {
            System.err.println("CAPI market: loginUrl manquante");
            return;
        }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(loginUrl));
                System.out.println("CAPI market: ouverture navigateur pour auth Frontier");
            } else {
                System.err.println("CAPI market: navigateur non supporté. URL: " + loginUrl);
            }
        } catch (Exception e) {
            System.err.println("CAPI market: erreur ouverture navigateur: " + e.getMessage());
        }
    }
}