package be.mirooz.elitedangerous.dashboard.service.webservice;

import be.mirooz.elitedangerous.backend.capi.CapiFacade;
import be.mirooz.elitedangerous.backend.capi.CapiServiceDownException;
import be.mirooz.elitedangerous.backend.capi.CapiWaitApprovalResponse;
import be.mirooz.elitedangerous.backend.capi.UnauthorizedException;
import be.mirooz.elitedangerous.backend.generated.model.CapiApiErrorBody;
import be.mirooz.elitedangerous.backend.generated.model.CapiFleetCarrierDto;
import be.mirooz.elitedangerous.backend.generated.model.CapiLoginDto;
import be.mirooz.elitedangerous.backend.generated.model.CapiDockedEvent;
import be.mirooz.elitedangerous.backend.generated.model.CapiMarketDto;
import be.mirooz.elitedangerous.backend.generated.model.CapiMarketProxyRequest;
import be.mirooz.elitedangerous.backend.generated.model.CapiProfileDto;
import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.service.CarrierTradeService;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.service.PreferencesService;
import be.mirooz.elitedangerous.dashboard.view.common.CapiAuthBrowserFallbackNotificationComponent;
import be.mirooz.elitedangerous.dashboard.view.common.CapiAuthConnectedNotificationComponent;
import be.mirooz.elitedangerous.dashboard.view.common.CapiAuthNotificationComponent;
import be.mirooz.elitedangerous.dashboard.view.common.CapiServiceDownNotificationComponent;
import be.mirooz.elitedangerous.dashboard.view.main.ConfigDialogController;
import be.mirooz.elitedangerous.dashboard.service.webservice.eddn.EddnUploader;
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
import java.util.function.Consumer;

public final class CapiApiService {

    /**
     * Résultat du test profil CAPI pour l’UI des paramètres (sans toast sur indisponibilité).
     */
    public enum CapiProfileSettingsStatus {
        CONNECTED,
        NOT_CONNECTED,
        SERVICE_DOWN
    }

    private static final CapiApiService INSTANCE = new CapiApiService();
    private static final long MARKET_MIN_INTERVAL_MS = 1000L;

    /**
     * Budget total côté client : le serveur tient 60 s, on relance tant que la somme reste sous ce plafond.
     */
    private static final long APPROVAL_WAIT_TOTAL_BUDGET_MS = TimeUnit.MINUTES.toMillis(15);
    /**
     * Petit backoff entre 2 tentatives en cas d'erreur réseau imprévue (hors timeout serveur normal).
     */
    private static final long APPROVAL_WAIT_ERROR_BACKOFF_MS = 2_000L;

    private final CapiFacade capiFacade = CapiFacade.getInstance();
    private final PreferencesService preferencesService = PreferencesService.getInstance();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Object marketRateLimitLock = new Object();
    private boolean authPromptVisible = false;
    private long lastMarketSendAtMs = 0L;
    /**
     * Empêche plusieurs boucles de long polling concurrentes pour un même commandant.
     */
    private final AtomicBoolean waitingForApproval = new AtomicBoolean(false);
    private final Object capiServiceDownNotifyLock = new Object();
    private long lastCapiServiceDownNotifMs;

    /**
     * Rappel UI après fin d’attente OAuth (bouton config) ; consommé une fois.
     */
    private Consumer<Boolean> loginApprovalUiCallback;

    private CapiApiService() {
    }

    public static CapiApiService getInstance() {
        return INSTANCE;
    }

    /**
     * Le backend CAPI exige le FID Frontier ; il n’est connu qu’après un événement journal {@code Commander}.
     */
    private boolean hasCommanderIdentityForCapi() {
        String fid = CommanderStatus.getInstance().getFID();
        return fid != null && !fid.isBlank();
    }

    public void sendMarketDatas(JsonNode journalDockedEvent) {
        if (!isCapiEnabled()) {
            return;
        }
        if (!hasCommanderIdentityForCapi()) {
            return;
        }
        if (DashboardContext.getInstance().isBatchLoading()) {
            return;
        }
        if (!canSendMarketNow()) {
            return;
        }
        try {
            CommanderStatus s = CommanderStatus.getInstance();
            CapiDockedEvent event = objectMapper.convertValue(
                    journalDockedEvent,
                    CapiDockedEvent.class
            );
            CapiMarketProxyRequest payload = new CapiMarketProxyRequest()
                    .fid(s.getFID())
                    .commanderName(s.getCommanderName())
                    .event(event);
            CapiMarketDto response = capiFacade.postMarket(payload);
            handleSuccess(payload, response, journalDockedEvent);
        } catch (UnauthorizedException e) {
            handleFrontierAuth(e.getError());
        } catch (CapiServiceDownException e) {
            notifyCapiServiceDownDebounced();
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
        if (!isCapiEnabled()) {
            return false;
        }
        if (!hasCommanderIdentityForCapi()) {
            return false;
        }
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
        } catch (CapiServiceDownException e) {
            notifyCapiServiceDownDebounced();
            return false;
        } catch (IOException e) {
            System.err.println("CAPI profile check failed: " + e.getMessage());
            promptAuthenticationApproval();
            return false;
        }
    }

    /**
     * État du profil CAPI pour la ligne de statut des paramètres.
     * Ne déclenche pas le toast « CAPI service down » (réservé aux autres flux).
     */
    public CapiProfileSettingsStatus getProfileSettingsStatus() {
        if (!isCapiEnabled()) {
            return CapiProfileSettingsStatus.NOT_CONNECTED;
        }
        if (!hasCommanderIdentityForCapi()) {
            return CapiProfileSettingsStatus.NOT_CONNECTED;
        }
        try {
            CommanderStatus status = CommanderStatus.getInstance();
            String language = getCurrentLanguage();
            CapiProfileDto profileResponse = capiFacade.fetchProfile(
                    status.getCommanderName(),
                    status.getFID(),
                    language
            );
            boolean ok = profileResponse != null
                    && isOauthApproved(profileResponse.getStatus(), profileResponse.getError());
            return ok ? CapiProfileSettingsStatus.CONNECTED : CapiProfileSettingsStatus.NOT_CONNECTED;
        } catch (UnauthorizedException e) {
            return CapiProfileSettingsStatus.NOT_CONNECTED;
        } catch (CapiServiceDownException e) {
            return CapiProfileSettingsStatus.SERVICE_DOWN;
        } catch (IOException e) {
            return CapiProfileSettingsStatus.NOT_CONNECTED;
        }
    }

    public void fetchFleetCarrierData() {
        if (!isCapiEnabled()) {
            return;
        }
        if (!hasCommanderIdentityForCapi()) {
            return;
        }
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
        } catch (CapiServiceDownException e) {
            notifyCapiServiceDownDebounced();
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

    private void handleSuccess(CapiMarketProxyRequest request, CapiMarketDto r, JsonNode rawDockedEvent) {
        if (r == null) {
            System.err.println("CAPI market: réponse vide");
            return;
        }
        if (!"success".equalsIgnoreCase(r.getStatus())) {
            System.err.println("CAPI market: réponse inattendue " + r);
            return;
        }
        if (r.getData() == null) {
            System.err.println("CAPI market: data absent");
            return;
        }
        Map<String, Object> capiMap = r.getData().getCapiMarket();
        if (capiMap == null || capiMap.isEmpty()) {
            System.err.println("CAPI market: capiMarket absent ou vide (backend trop ancien ?)");
            return;
        }
        JsonNode capiNode = objectMapper.valueToTree(capiMap);
        JsonNode dockedNode = objectMapper.valueToTree(request.getEvent());
        String fid = r.getData().getFid();
        EddnUploader.getInstance().publishCapiMarketSnapshot(fid, dockedNode, capiNode);
        System.out.println("CAPI market: snapshot reçu, envoi EDDN commodity/3 (Warboard)");
    }

    // =========================
    // AUTH
    // =========================

    private void handleFrontierAuth(CapiApiErrorBody error) {
        if (error == null) {
            System.err.println("CAPI : réponse auth vide");
            return;
        }

        Map<String, Object> details = error.getDetails();
        if (details == null) {
            System.err.println("CAPI : détails auth absents");
            return;
        }

        String action = (String) details.get("action");

        if ("authenticate_frontier".equalsIgnoreCase(action)) {
            String errorMessage = error.getMessage();

            if (errorMessage != null && !errorMessage.isBlank()) {
                System.err.println("CAPI : " + errorMessage);
            }

            promptAuthenticationApproval();
            return;
        }

        System.err.println("CAPI : réponse auth inattendue " + error);
    }

    // =========================
    // BROWSER
    // =========================

    private void promptAuthenticationApproval() {
        if (!isCapiEnabled()) {
            return;
        }
        if (!hasCommanderIdentityForCapi()) {
            return;
        }
        Platform.runLater(() -> {
            try {
                Window ownerWindow = getBestOwnerWindow();
                if (!(ownerWindow instanceof Stage ownerStage)) {
                    System.err.println("CAPI : aucune fenêtre active trouvée pour afficher la popup d'authentification");
                    return;
                }

                Scene scene = ownerStage.getScene();
                StackPane popupContainer = findPopupContainer(scene != null ? scene.getRoot() : null);
                if (popupContainer == null) {
                    System.err.println("CAPI : popupContainer introuvable");
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
                        this::requestAuthenticationAndOpenBrowser,
                        this::onCapiAuthDeclined
                );
                notification.opacityProperty().addListener((obs, oldValue, newValue) -> {
                    if (newValue.doubleValue() == 0.0) {
                        authPromptVisible = false;
                    }
                });
            } catch (Exception e) {
                authPromptVisible = false;
                System.err.println("CAPI : erreur affichage popup authentification: " + e.getMessage());
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

    private static final long CAPI_SERVICE_DOWN_NOTIF_MIN_INTERVAL_MS = 15_000L;

    /**
     * Toast d’avertissement centré ; anti-spam si plusieurs appels CAPI renvoient 418 en rafale.
     */
    private void notifyCapiServiceDownDebounced() {
        long now = System.currentTimeMillis();
        synchronized (capiServiceDownNotifyLock) {
            if (now - lastCapiServiceDownNotifMs < CAPI_SERVICE_DOWN_NOTIF_MIN_INTERVAL_MS) {
                return;
            }
            lastCapiServiceDownNotifMs = now;
        }
        Platform.runLater(() -> {
            try {
                Window ownerWindow = getBestOwnerWindow();
                if (!(ownerWindow instanceof Stage ownerStage)) {
                    System.err.println("CAPI service down : aucune fenêtre pour la notification");
                    return;
                }
                Scene scene = ownerStage.getScene();
                StackPane popupContainer = findPopupContainer(scene != null ? scene.getRoot() : null);
                if (popupContainer == null) {
                    System.err.println("CAPI service down : popupContainer introuvable");
                    return;
                }
                new CapiServiceDownNotificationComponent(popupContainer);
            } catch (Exception e) {
                System.err.println("CAPI service down : erreur affichage notification: " + e.getMessage());
            }
        });
    }

    private void onCapiAuthDeclined() {
        boolean wasEnabled = preferencesService.isCapiLoginEnabled();
        if (wasEnabled) {
            String fid = CommanderStatus.getInstance().getFID();
            if (fid != null && !fid.isBlank()) {
                final String fidForLogout = fid;
                Thread t = new Thread(() -> {
                    try {
                        CapiFacade.getInstance().logout(fidForLogout);
                    } catch (IOException | InterruptedException e) {
                        if (e instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                        System.err.println("CAPI logout: " + e.getMessage());
                    }
                }, "capi-logout");
                t.setDaemon(true);
                t.start();
            }
        }
        preferencesService.setCapiLoginEnabled(false);
        ConfigDialogController.applyCapiLoginPreferenceToOpenConfigDialog();
    }

    private void requestAuthenticationAndOpenBrowser() {
        CommanderStatus status = CommanderStatus.getInstance();
        String fid = status.getFID();
        if (fid == null || fid.isBlank()) {
            System.err.println("CAPI : FID manquant, impossible de demander l'authentification");
            notifyLoginApprovalUiCallback(false);
            return;
        }

        try {
            CapiLoginDto response = capiFacade.requestAuthentication(fid);
            String loginUrl = extractLoginUrl(response);
            if (!openBrowser(loginUrl) && loginUrl != null && !loginUrl.isBlank()) {
                showCapiAuthBrowserFallbackNotification(loginUrl);
            }
            startWaitingForApproval(fid);
        } catch (UnauthorizedException e) {
            handleFrontierAuth(e.getError());
            notifyLoginApprovalUiCallback(false);
        } catch (CapiServiceDownException e) {
            notifyCapiServiceDownDebounced();
            notifyLoginApprovalUiCallback(false);
        } catch (IOException e) {
            System.err.println("CAPI : erreur requestAuthentication: " + e.getMessage());
            notifyLoginApprovalUiCallback(false);
        }
    }


    /**
     * Ouvre le navigateur OAuth puis attend l’approbation ; appelle {@code afterWaitApprovalUi} sur le fil JavaFX
     * ({@code true} si approuvé, {@code false} sinon : échec, timeout, ou attente déjà en cours).
     */
    public void loginCapiAccount(Consumer<Boolean> afterWaitApprovalUi) {
        loginApprovalUiCallback = afterWaitApprovalUi;
        requestAuthenticationAndOpenBrowser();
    }

    private boolean isCapiEnabled() {
        return preferencesService.isCapiLoginEnabled();
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
            notifyLoginApprovalUiCallback(false);
            return;
        }
        if (!waitingForApproval.compareAndSet(false, true)) {
            notifyLoginApprovalUiCallback(false);
            return;
        }
        Thread worker = new Thread(() -> runApprovalWaitLoop(fid), "capi-wait-approval");
        worker.setDaemon(true);
        worker.start();
    }

    private void notifyLoginApprovalUiCallback(boolean ok) {
        Consumer<Boolean> cb = loginApprovalUiCallback;
        loginApprovalUiCallback = null;
        if (cb == null) {
            return;
        }
        Platform.runLater(() -> cb.accept(ok));
    }

    private void runApprovalWaitLoop(String fid) {
        try {
            long deadlineMs = System.currentTimeMillis() + APPROVAL_WAIT_TOTAL_BUDGET_MS;
            while (System.currentTimeMillis() < deadlineMs) {
                try {
                    CapiWaitApprovalResponse resp = capiFacade.waitApproval(fid);
                    if (resp.isApproved()) {
                        System.out.println("CAPI : approbation reçue via wait-approval (fid=" + fid + ")");
                        onApprovalReceived();
                        notifyLoginApprovalUiCallback(true);
                        return;
                    }
                    // approved=false (timeout serveur attendu ou réponse négative) → on reboucle sans attendre
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    notifyLoginApprovalUiCallback(false);
                    return;
                } catch (CapiServiceDownException e) {
                    notifyCapiServiceDownDebounced();
                    notifyLoginApprovalUiCallback(false);
                    return;
                } catch (IOException ioe) {
                    System.err.println("CAPI : wait-approval erreur: " + ioe.getMessage());
                    try {
                        Thread.sleep(APPROVAL_WAIT_ERROR_BACKOFF_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        notifyLoginApprovalUiCallback(false);
                        return;
                    }
                }
            }
            System.out.println("CAPI : wait-approval budget expiré (15 min), réaffichage de la popup");
            promptAuthenticationApproval();
            notifyLoginApprovalUiCallback(false);
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
                System.err.println("CAPI : erreur affichage toast CAPI connected: " + e.getMessage());
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

    /**
     * @return {@code true} si une tentative d’ouverture du navigateur a réussi
     */
    private boolean openBrowser(String loginUrl) {
        if (loginUrl == null || loginUrl.isBlank()) {
            System.err.println("CAPI : loginUrl manquante");
            return false;
        }
        final URI uri;
        try {
            uri = URI.create(loginUrl);
        } catch (IllegalArgumentException e) {
            System.err.println("CAPI : loginUrl invalide: " + e.getMessage());
            return false;
        }
        if (tryOpenBrowserWithDesktop(uri)) {
            System.out.println("CAPI : ouverture navigateur pour auth Frontier");
            return true;
        }
        if (tryOpenBrowserWithSystemLauncher(loginUrl)) {
            System.out.println("CAPI : ouverture navigateur pour auth Frontier (xdg-open / équivalent)");
            return true;
        }
        System.err.println("CAPI : impossible d'ouvrir le navigateur automatiquement (URL affichée dans la notification)");
        return false;
    }

    private void showCapiAuthBrowserFallbackNotification(String loginUrl) {
        Platform.runLater(() -> {
            try {
                Window ownerWindow = getBestOwnerWindow();
                if (!(ownerWindow instanceof Stage ownerStage)) {
                    System.err.println("CAPI : aucune fenêtre pour afficher l’URL d’auth");
                    System.err.println("CAPI : ouvrez manuellement : " + loginUrl);
                    return;
                }
                Scene scene = ownerStage.getScene();
                StackPane popupContainer = findPopupContainer(scene != null ? scene.getRoot() : null);
                if (popupContainer == null) {
                    System.err.println("CAPI : popupContainer introuvable pour l’URL d’auth");
                    System.err.println("CAPI : ouvrez manuellement : " + loginUrl);
                    return;
                }
                ownerStage.toFront();
                new CapiAuthBrowserFallbackNotificationComponent(popupContainer, loginUrl);
            } catch (Exception e) {
                System.err.println("CAPI : erreur notification URL navigateur: " + e.getMessage());
                System.err.println("CAPI : ouvrez manuellement : " + loginUrl);
            }
        });
    }

    /**
     * Sur Linux, {@link Desktop#isDesktopSupported()} peut être vrai alors que {@link Desktop.Action#BROWSE}
     * n'est pas disponible (message « The BROWSE action is not supported on the current platform! »).
     */
    private static boolean tryOpenBrowserWithDesktop(URI uri) {
        if (!Desktop.isDesktopSupported()) {
            return false;
        }
        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.BROWSE)) {
            return false;
        }
        try {
            desktop.browse(uri);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean tryOpenBrowserWithSystemLauncher(String url) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        ProcessBuilder pb;
        if (os.contains("win")) {
            pb = new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url);
        } else if (os.contains("mac")) {
            pb = new ProcessBuilder("open", url);
        } else {
            pb = new ProcessBuilder("xdg-open", url);
        }
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        try {
            pb.start();
            return true;
        } catch (IOException e) {
            System.err.println("CAPI : erreur ouverture navigateur (fallback): " + e.getMessage());
            return false;
        }
    }

    /**
     * Réponse CAPI considérée valide côté OAuth (profil, fleet carrier, etc.).
     */
    private static boolean isOauthApproved(String status, CapiApiErrorBody error) {
        return status != null
                && ("success".equalsIgnoreCase(status) || "ok".equalsIgnoreCase(status))
                && error == null;
    }
}