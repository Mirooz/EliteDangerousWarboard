package be.mirooz.elitedangerous.dashboard.service.webservice;

import be.mirooz.elitedangerous.backend.capi.CapiFacade;
import be.mirooz.elitedangerous.backend.capi.CapiWaitApprovalResponse;
import be.mirooz.elitedangerous.backend.capi.UnauthorizedException;
import be.mirooz.elitedangerous.backend.generated.model.CapiApiErrorBody;
import be.mirooz.elitedangerous.backend.generated.model.CapiFleetCarrierDto;
import be.mirooz.elitedangerous.backend.generated.model.CapiLoginDto;
import be.mirooz.elitedangerous.backend.generated.model.CapiMarketDto;
import be.mirooz.elitedangerous.backend.generated.model.CapiMarketEvent;
import be.mirooz.elitedangerous.backend.generated.model.CapiMarketProxyRequest;
import be.mirooz.elitedangerous.backend.generated.model.CapiProfileDto;
import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.service.CarrierTradeService;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.view.common.CapiAuthConnectedNotificationComponent;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CapiApiService {

    private static final CapiApiService INSTANCE = new CapiApiService();
    private static final long MARKET_MIN_INTERVAL_MS = 1000L;

    /** Budget total côté client : le serveur tient 60 s, on relance tant que la somme reste sous ce plafond. */
    private static final long APPROVAL_WAIT_TOTAL_BUDGET_MS = TimeUnit.MINUTES.toMillis(15);
    /** Petit backoff entre 2 tentatives en cas d'erreur réseau imprévue (hors timeout serveur normal). */
    private static final long APPROVAL_WAIT_ERROR_BACKOFF_MS = 2_000L;

    private final CapiFacade capiFacade = CapiFacade.getInstance();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Object marketRateLimitLock = new Object();
    private boolean authPromptVisible = false;
    private long lastMarketSendAtMs = 0L;
    /** Empêche plusieurs boucles de long polling concurrentes pour un même commandant. */
    private final AtomicBoolean waitingForApproval = new AtomicBoolean(false);

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
            CapiMarketDto response = capiFacade.postMarket(payload);
            handleSuccess(response);
        } catch (UnauthorizedException e) {
            handleFrontierAuth(e.getError());
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

    public boolean checkCapiAuthentication() {
        try {
            CommanderStatus status = CommanderStatus.getInstance();
            String language = getCurrentLanguage();
            CapiProfileDto profileResponse = capiFacade.fetchProfile(
                    status.getCommanderName(),
                    status.getFID(),
                    language
            );
            boolean profileOk = profileResponse != null
                    && isOauthApproved(profileResponse.getStatus(), profileResponse.getError());
            if (!profileOk) {
                promptAuthenticationApproval();
            }
            return profileOk;
        } catch (UnauthorizedException e) {
            handleFrontierAuth(e.getError());
            return false;
        } catch (IOException e) {
            System.err.println("CAPI profile check failed: " + e.getMessage());
            promptAuthenticationApproval();
            return false;
        }
    }

    public void fetchFleetCarrierData() {
        try {
            CommanderStatus status = CommanderStatus.getInstance();
            String language = getCurrentLanguage();
            CapiFleetCarrierDto fleetCarrierResponse = capiFacade.fetchFleetCarrier(
                    status.getCommanderName(),
                    status.getFID(),
                    language
            );
            if (fleetCarrierResponse == null
                    || !isOauthApproved(fleetCarrierResponse.getStatus(), fleetCarrierResponse.getError())) {
                return;
            }
            CarrierTradeService carrierTrade = CarrierTradeService.getInstance();
            carrierTrade.applyFleetCarrierCapiSnapshot(fleetCarrierResponse.getData());
            System.out.println("CAPI fleet carrier synced: " + carrierTrade.getCarrierStatus());
        } catch (UnauthorizedException e) {
            handleFrontierAuth(e.getError());
        } catch (IOException e) {
            System.err.println("CAPI fleet carrier fetch failed: " + e.getMessage());
        }
    }

    private String getCurrentLanguage() {
        return LocalizationService.getInstance().getCurrentLocale() != null
                ? LocalizationService.getInstance().getCurrentLocale().getLanguage()
                : Locale.ENGLISH.getLanguage();
    }

    // =========================
    // SUCCESS
    // =========================

    private void handleSuccess(CapiMarketDto r) {
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

    private void handleFrontierAuth(CapiApiErrorBody error) {
        if (error == null) {
            System.err.println("CAPI market: réponse auth vide");
            return;
        }

        Map<String, Object> details = error.getDetails();
        if (details == null) {
            System.err.println("CAPI market: détails auth absents");
            return;
        }

        String action = (String) details.get("action");

        if ("authenticate_frontier".equalsIgnoreCase(action)) {
            String errorMessage = error.getMessage();

            if (errorMessage != null && !errorMessage.isBlank()) {
                System.err.println("CAPI market: " + errorMessage);
            }

            promptAuthenticationApproval();
            return;
        }

        System.err.println("CAPI market: réponse auth inattendue " + error);
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
            CapiLoginDto response = capiFacade.requestAuthentication(fid);
            String loginUrl = extractLoginUrl(response);
            openBrowser(loginUrl);
            startWaitingForApproval(fid);
        } catch (UnauthorizedException e) {
            handleFrontierAuth(e.getError());
        } catch (IOException e) {
            System.err.println("CAPI market: erreur requestAuthentication: " + e.getMessage());
        }
    }

    /**
     * Lance une boucle de long polling sur {@code GET /api/capi/wait-approval} (hold serveur 60 s)
     * jusqu'à réception de {@code approved=true} ou expiration du budget total de 15 min.
     *
     * <ul>
     *     <li>Sur succès : rafraîchit le fleet carrier et affiche un toast « CAPI connected ».</li>
     *     <li>Sur expiration : réaffiche la popup d'approbation via {@link #promptAuthenticationApproval()}.</li>
     *     <li>Une seule boucle active par instance : les appels concurrents sont ignorés.</li>
     * </ul>
     */
    private void startWaitingForApproval(String fid) {
        if (fid == null || fid.isBlank()) {
            return;
        }
        if (!waitingForApproval.compareAndSet(false, true)) {
            return;
        }
        Thread worker = new Thread(() -> runApprovalWaitLoop(fid), "capi-wait-approval");
        worker.setDaemon(true);
        worker.start();
    }

    private void runApprovalWaitLoop(String fid) {
        try {
            long deadlineMs = System.currentTimeMillis() + APPROVAL_WAIT_TOTAL_BUDGET_MS;
            while (System.currentTimeMillis() < deadlineMs) {
                try {
                    CapiWaitApprovalResponse resp = capiFacade.waitApproval(fid);
                    if (resp.isApproved()) {
                        System.out.println("CAPI market: approbation reçue via wait-approval (fid=" + fid + ")");
                        onApprovalReceived();
                        return;
                    }
                    // approved=false (timeout serveur attendu ou réponse négative) → on reboucle sans attendre
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (IOException ioe) {
                    System.err.println("CAPI market: wait-approval erreur: " + ioe.getMessage());
                    try {
                        Thread.sleep(APPROVAL_WAIT_ERROR_BACKOFF_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
            System.out.println("CAPI market: wait-approval budget expiré (15 min), réaffichage de la popup");
            promptAuthenticationApproval();
        } finally {
            waitingForApproval.set(false);
        }
    }

    private void onApprovalReceived() {
        fetchFleetCarrierData();
        Platform.runLater(() -> {
            try {
                Window ownerWindow = getBestOwnerWindow();
                if (!(ownerWindow instanceof Stage ownerStage)) {
                    return;
                }
                Scene scene = ownerStage.getScene();
                StackPane popupContainer = findPopupContainer(scene != null ? scene.getRoot() : null);
                if (popupContainer == null) {
                    return;
                }
                new CapiAuthConnectedNotificationComponent(popupContainer);
            } catch (Exception e) {
                System.err.println("CAPI market: erreur affichage toast CAPI connected: " + e.getMessage());
            }
        });
    }

    private String extractLoginUrl(CapiLoginDto response) {
        if (response == null) {
            return null;
        }

        if (response.getData() != null && response.getData().getLoginUrl() != null) {
            String url = response.getData().getLoginUrl();
            if (!url.isBlank()) {
                return url;
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

    /** Réponse CAPI considérée valide côté OAuth (profil, fleet carrier, etc.). */
    private static boolean isOauthApproved(String status, CapiApiErrorBody error) {
        return status != null
                && ("success".equalsIgnoreCase(status) || "ok".equalsIgnoreCase(status))
                && error == null;
    }
}