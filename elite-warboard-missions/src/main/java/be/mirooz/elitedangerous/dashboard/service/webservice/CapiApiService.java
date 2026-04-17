package be.mirooz.elitedangerous.dashboard.service.webservice;

import be.mirooz.elitedangerous.backend.capi.CapiFacade;
import be.mirooz.elitedangerous.backend.capi.UnauthorizedException;
import be.mirooz.elitedangerous.backend.generated.model.CapiApiResponse;
import be.mirooz.elitedangerous.backend.generated.model.CapiMarketEvent;
import be.mirooz.elitedangerous.backend.generated.model.CapiMarketProxyRequest;
import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.view.common.CapiAuthNotificationComponent;
import be.mirooz.elitedangerous.dashboard.view.common.context.DashboardContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.Map;

public final class CapiApiService {

    private static final CapiApiService INSTANCE = new CapiApiService();
    private static final long MARKET_MIN_INTERVAL_MS = 1000L;

    private final CapiFacade capiFacade = CapiFacade.getInstance();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Object marketRateLimitLock = new Object();
    private boolean authPromptVisible = false;
    private long lastMarketSendAtMs = 0L;

    private CapiApiService() {}

    public static CapiApiService getInstance() {
        return INSTANCE;
    }

    public void sendMarketDatas(JsonNode journalMarketEvent) {
        if (DashboardContext.getInstance().isBatchLoading()) {
            return;
        }
        if (!canSendMarketNow()) {
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
            CapiApiResponse response = capiFacade.postMarket(payload);
            handleSuccess(response);
        } catch (UnauthorizedException e) {
            handleFrontierAuth(e.getResponse());
        } catch (IOException e) {
            System.err.println("Market send error: " + e.getMessage());
        }
    }

    private boolean canSendMarketNow() {
        long now = System.currentTimeMillis();
        synchronized (marketRateLimitLock) {
            if (now - lastMarketSendAtMs < MARKET_MIN_INTERVAL_MS) {
                return false;
            }
            lastMarketSendAtMs = now;
            return true;
        }
    }

    public void showStartupAuthenticationPromptIfNeeded() {
        new Thread(() -> {
            try {
                CommanderStatus status = CommanderStatus.getInstance();
                String language = LocalizationService.getInstance().getCurrentLocale() != null
                        ? LocalizationService.getInstance().getCurrentLocale().getLanguage()
                        : Locale.ENGLISH.getLanguage();
                CapiApiResponse profileResponse = capiFacade.fetchProfile(
                        status.getCommanderName(),
                        status.getFID(),
                        language
                );
                if (!isProfileResponseOk(profileResponse)) {
                    promptAuthenticationApproval();
                }
            } catch (IOException e) {
                System.err.println("CAPI profile check failed at startup: " + e.getMessage());
                promptAuthenticationApproval();
            }
        }, "capi-profile-check").start();
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
            String errorMessage = response.getError().getMessage();

            if (errorMessage != null && !errorMessage.isBlank()) {
                System.err.println("CAPI market: " + errorMessage);
            }

            promptAuthenticationApproval();
            return;
        }

        System.err.println("CAPI market: réponse auth inattendue " + response);
    }

    // =========================
    // BROWSER
    // =========================

    private void promptAuthenticationApproval() {
        Platform.runLater(() -> {
            try {
                Window ownerWindow = getBestOwnerWindow();
                if (!(ownerWindow instanceof Stage ownerStage)) {
                    System.err.println("CAPI market: aucune fenêtre active trouvée pour afficher la popup d'authentification");
                    return;
                }

                Scene scene = ownerStage.getScene();
                StackPane popupContainer = findPopupContainer(scene != null ? scene.getRoot() : null);
                if (popupContainer == null) {
                    System.err.println("CAPI market: popupContainer introuvable");
                    return;
                }

                if (authPromptVisible) {
                    return;
                }
                authPromptVisible = true;
                ownerStage.toFront();
                ownerStage.requestFocus();

                CapiAuthNotificationComponent notification = new CapiAuthNotificationComponent(
                        popupContainer,
                        this::requestAuthenticationAndOpenBrowser
                );
                notification.opacityProperty().addListener((obs, oldValue, newValue) -> {
                    if (newValue.doubleValue() == 0.0) {
                        authPromptVisible = false;
                    }
                });
            } catch (Exception e) {
                authPromptVisible = false;
                System.err.println("CAPI market: erreur affichage popup authentification: " + e.getMessage());
            }
        });
    }

    private Window getBestOwnerWindow() {
        return Stage.getWindows().stream()
                .filter(Window::isShowing)
                .sorted((w1, w2) -> Boolean.compare(w2.isFocused(), w1.isFocused()))
                .findFirst()
                .orElse(null);
    }

    private StackPane findPopupContainer(Node root) {
        if (root == null) {
            return null;
        }
        if (root instanceof StackPane stackPane && "popupContainer".equals(root.getId())) {
            return stackPane;
        }
        if (root instanceof javafx.scene.layout.Pane pane) {
            for (Node child : pane.getChildren()) {
                StackPane found = findPopupContainer(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private void requestAuthenticationAndOpenBrowser() {
        CommanderStatus status = CommanderStatus.getInstance();
        String fid = status.getFID();
        if (fid == null || fid.isBlank()) {
            System.err.println("CAPI market: FID manquant, impossible de demander l'authentification");
            return;
        }

        try {
            CapiApiResponse response = capiFacade.requestAuthentication(fid);
            String loginUrl = extractLoginUrl(response);
            openBrowser(loginUrl);
        } catch (IOException e) {
            System.err.println("CAPI market: erreur requestAuthentication: " + e.getMessage());
        }
    }

    private String extractLoginUrl(CapiApiResponse response) {
        if (response == null) {
            return null;
        }

        Object data = response.getData();
        if (data instanceof Map<?, ?> dataMap) {
            Object loginUrl = dataMap.get("loginUrl");
            if (loginUrl instanceof String loginUrlString) {
                return loginUrlString;
            }
        }

        if (response.getError() != null && response.getError().getDetails() != null) {
            Object loginUrl = response.getError().getDetails().get("loginUrl");
            if (loginUrl instanceof String loginUrlString) {
                return loginUrlString;
            }
        }

        return null;
    }

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

    private boolean isProfileResponseOk(CapiApiResponse response) {
        if (response == null) {
            return false;
        }
        String status = response.getStatus();
        return status != null
                && ("success".equalsIgnoreCase(status) || "ok".equalsIgnoreCase(status))
                && response.getError() == null;
    }
}