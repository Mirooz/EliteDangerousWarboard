package be.mirooz.elitedangerous.dashboard.view.main;

import be.mirooz.elitedangerous.dashboard.service.*;
import be.mirooz.elitedangerous.dashboard.service.persistence.PersistenceService;
import be.mirooz.elitedangerous.dashboard.service.webservice.AnalyticsService;
import be.mirooz.elitedangerous.dashboard.view.common.CommanderStatusComponent;
import be.mirooz.elitedangerous.dashboard.view.common.DialogComponent;
import be.mirooz.elitedangerous.dashboard.view.common.managers.UIManager;
import be.mirooz.elitedangerous.dashboard.view.common.managers.PopupManager;
import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.service.journal.watcher.JournalTailService;
import be.mirooz.elitedangerous.dashboard.service.journal.watcher.JournalWatcherService;
import be.mirooz.elitedangerous.dashboard.view.common.IRefreshable;
import be.mirooz.elitedangerous.dashboard.view.common.IBatchListener;
import be.mirooz.elitedangerous.dashboard.view.combat.HeaderController;
import be.mirooz.elitedangerous.dashboard.view.combat.MissionListController;
import be.mirooz.elitedangerous.dashboard.view.combat.DestroyedShipsController;
import be.mirooz.elitedangerous.dashboard.view.mining.MiningController;
import be.mirooz.elitedangerous.dashboard.view.colonisation.ColonisationPanelController;
import be.mirooz.elitedangerous.dashboard.view.exploration.ExplorationController;
import be.mirooz.elitedangerous.dashboard.view.common.managers.CopyClipboardManager;
import be.mirooz.elitedangerous.dashboard.window.PrimaryWindowChromeSupport;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.CacheHint;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;

import java.awt.Desktop;
import java.net.URI;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * Contrôleur principal du dashboard Elite Dangerous
 */
public class DashboardController implements Initializable , IRefreshable, IBatchListener{

    public Label appTitleLabel;
    public Label appSubtitleLabel;
    public Label statusLabel;
    public Button configButton;
    public ImageView donateButtonImage;
    @FXML
    private TabPane mainTabPane;

    @FXML
    private Button globalModuleTutorialButton;

    @FXML
    private Tab missionsTab;
    
    @FXML
    private Tab miningTab;
    
    @FXML
    private Tab explorationTab;

    @FXML
    private StackPane missionsTabStackHost;

    @FXML
    private Tab colonisationTab;
    
    @FXML
    private ImageView missionsTabImage;
    
    @FXML
    private ImageView miningTabImage;
    
    @FXML
    private ImageView explorationTabImage;

    @FXML
    private ImageView colonisationTabImage;
    
    @FXML
    private BorderPane missionsPane;
    
    @FXML
    private BorderPane miningPane;
    
    @FXML
    private BorderPane explorationPane;

    @FXML
    private BorderPane colonisationPane;

    @FXML
    private StackPane popupContainer;

    @FXML
    private HBox dashboardTitleBar;
    @FXML
    private Button windowMinimizeButton;
    @FXML
    private Button windowMaxRestoreButton;
    @FXML
    private Button windowCloseButton;

    private ColonisationPanelController colonisationPanelController;

    private HeaderController missionHeaderController;
    private MissionListController missionListController;

    private PrimaryWindowChromeSupport primaryWindowChromeSupport;

    private final ChangeListener<Tab> colonisationTabSelectionListener = (obs, oldTab, newTab) -> {
        if (newTab == colonisationTab && colonisationPanelController != null) {
            colonisationPanelController.refreshAll();
        }
    };
    
    // Labels pour commander, system, station (déplacés du footer vers le header)
    @FXML
    private Label commanderLabel;
    @FXML
    private Label systemLabel;
    @FXML
    private Label stationLabel;
    @FXML
    private Label commanderHeaderLabel;
    @FXML
    private Label systemHeaderLabel;
    @FXML
    private Label stationHeaderLabel;
    
    private final DashboardService dashboardService = DashboardService.getInstance();
    private final PopupManager popupManager = PopupManager.getInstance();
    private final CommanderStatusComponent commanderStatusComponent= CommanderStatusComponent.getInstance();
    private final WindowToggleService windowToggleService = WindowToggleService.getInstance();
    private final LocalizationService localizationService = LocalizationService.getInstance();
    private final CopyClipboardManager copyClipboardManager = CopyClipboardManager.getInstance();
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadComponents();
        initializeTabImages();
        loadDonateButtonImage();
        popupManager.attachToContainer(popupContainer);
        UIManager.getInstance().register(this);
        updateTranslations();
        initializeCommanderStatusLabels();

        // Écouter les changements de langue
        localizationService.addLanguageChangeListener(locale -> updateTranslations());
        
        // Initialiser le TabPane dans le service de bind unifié
        windowToggleService.initializeTabPane(mainTabPane, missionsTab, miningTab, explorationTab, colonisationTab);
        mainTabPane.getSelectionModel().selectedItemProperty().addListener(colonisationTabSelectionListener);
        installGlobalModuleTutorialControl();
    }

    private void installGlobalModuleTutorialControl() {
        if (globalModuleTutorialButton == null) {
            return;
        }
        globalModuleTutorialButton.setFocusTraversable(false);
        refreshGlobalModuleTutorialBar();
    }

    private void refreshGlobalModuleTutorialBar() {
        if (globalModuleTutorialButton == null || mainTabPane == null) {
            return;
        }
        Tab sel = mainTabPane.getSelectionModel().getSelectedItem();
        boolean missions = sel == missionsTab;
        globalModuleTutorialButton.setText(localizationService.getString("tutorial.global.button"));
        globalModuleTutorialButton.setDisable(!missions);
        Tooltip tip = new Tooltip();
        tip.setWrapText(true);
        tip.setMaxWidth(400);
        tip.setText(missions
                ? localizationService.getString("tutorial.missions.launch.tooltip")
                : localizationService.getString("tutorial.global.unavailable"));
        globalModuleTutorialButton.setTooltip(tip);
    }

    @FXML
    private void onGlobalModuleTutorialClicked() {
        Tab sel = mainTabPane.getSelectionModel().getSelectedItem();
        if (sel == missionsTab && missionListController != null) {
            missionListController.launchMissionTutorial();
        }
    }

    /**
     * Référence le {@link Stage} principal : à l’init FXML, {@link javafx.scene.Scene#getWindow()} est encore
     * souvent null ; {@link be.mirooz.elitedangerous.dashboard.EliteDashboardApp} appelle cette méthode
     * juste après {@link Stage#setScene(javafx.scene.Scene)}.
     */
    public void attachPrimaryStage(Stage stage) {
        Objects.requireNonNull(stage, "stage");
        Platform.runLater(() -> {
            if (primaryWindowChromeSupport != null) {
                return;
            }
            primaryWindowChromeSupport = new PrimaryWindowChromeSupport(
                    stage,
                    null,
                    null,
                    dashboardTitleBar,
                    null,
                    windowMinimizeButton,
                    windowMaxRestoreButton,
                    windowCloseButton,
                    configButton,
                    donateButtonImage,
                    systemLabel,
                    localizationService);
            primaryWindowChromeSupport.installIfNeeded();
        });
    }
    
    /**
     * Initialise les labels de statut du commandant (commander, system, station)
     */
    private void initializeCommanderStatusLabels() {
        // Ajouter la fonctionnalité de copie au clic sur le système
        if (systemLabel != null) {
            systemLabel.setOnMouseClicked(this::onSystemLabelClicked);
            // S'assurer que les classes CSS sont dans le bon ordre
            systemLabel.getStyleClass().clear();
            systemLabel.getStyleClass().addAll("clickable-system-source", "footer-value");
        }
        // Le binding ne met à jour que la couleur ; LoadGame / Shutdown changent isOnline hors onBatchEnd
        // (on resynchronise explicitement à la fin du batch journal, voir {@link #onBatchEnd()}).
        commanderStatusComponent.getIsOnline().addListener((obs, wasOnline, onlineNow) -> {
            if (statusLabel != null) {
                updateStatusLabel();
            }
        });
    }

    private void loadComponents() {
        try {

            dashboardService.addBatchListener(this);
            // Charger l'onglet Missions
            createHeaderPanel();
            createMissionPanel();
            createDestroyedShipsPanel();
            
            // Charger l'onglet Mining
            createMiningPanel();
            
            // Charger l'onglet Exploration
            createExplorationPanel();

            createColonisationPanel();

        } catch (IOException e) {
            System.err.println("Erreur lors du chargement des composants: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void createHeaderPanel() throws IOException {
        FXMLLoader headerLoader = new FXMLLoader(getClass().getResource("/fxml/combat/header.fxml"));
        VBox header = headerLoader.load();
        missionHeaderController = headerLoader.getController();
        missionsPane.setTop(header);
    }


    private void createMissionPanel() throws IOException {
        FXMLLoader missionListLoader = new FXMLLoader(getClass().getResource("/fxml/combat/mission-list.fxml"));
        Parent missionList = missionListLoader.load();
        missionListController = missionListLoader.getController();
        dashboardService.addBatchListener(missionListController);
        missionsPane.setCenter(missionList);
        if (missionsTabStackHost != null && missionListController != null) {
            missionListController.configureMissionsTutorial(missionsTabStackHost, missionHeaderController);
        }
    }

    private void createDestroyedShipsPanel() throws IOException {
        FXMLLoader destroyedShipsLoader = new FXMLLoader(getClass().getResource("/fxml/combat/destroyed-ships.fxml"));
        VBox destroyedShipsPanel = destroyedShipsLoader.load();
        DestroyedShipsController destroyedShipsController = destroyedShipsLoader.getController();
        dashboardService.addBatchListener(destroyedShipsController);
        missionsPane.setLeft(destroyedShipsPanel);
    }

    private void createMiningPanel() throws IOException {
        // Charger le panneau de mining
        FXMLLoader miningLoader = new FXMLLoader(getClass().getResource("/fxml/mining/mining-panel.fxml"));
        VBox miningPanel = miningLoader.load();
        MiningController miningController = miningLoader.getController();
        miningPane.setCenter(miningPanel);
    }

    private void createExplorationPanel() throws IOException {
        // Charger le panneau d'exploration
        FXMLLoader explorationLoader = new FXMLLoader(getClass().getResource("/fxml/exploration/exploration-panel.fxml"));
        VBox explorationPanel = explorationLoader.load();
        ExplorationController explorationController = explorationLoader.getController();
        explorationPane.setCenter(explorationPanel);
    }

    private void createColonisationPanel() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/colonisation/colonisation-panel.fxml"));
        VBox panel = loader.load();
        colonisationPanelController = loader.getController();
        colonisationPane.setCenter(panel);
    }

    /**
     * Initialise les images des onglets avec design Elite Dangerous
     */
    private void initializeTabImages() {
        try {
            // Charger l'image pour l'onglet Missions (Empire)
            Image empireImage = new Image(getClass().getResourceAsStream("/images/dashboard/elitewarboard.png"));
            missionsTabImage.setImage(empireImage);
            missionsTabImage.setFitWidth(60); // Taille carrée élégante
            missionsTabImage.setFitHeight(60); // Taille carrée élégante
            missionsTabImage.setPreserveRatio(true); // Conserver les proportions
            missionsTabImage.setSmooth(true);
            
            // Charger l'image pour l'onglet Mining (Minageship)
            Image minageshipImage = new Image(getClass().getResourceAsStream("/images/dashboard/mining.png"));
            miningTabImage.setImage(minageshipImage);
            miningTabImage.setFitWidth(60); // Taille carrée élégante
            miningTabImage.setFitHeight(60); // Taille carrée élégante
            miningTabImage.setPreserveRatio(true); // Conserver les proportions
            miningTabImage.setSmooth(true);
            
            // Charger l'image pour l'onglet Exploration
            Image explorationImage = new Image(getClass().getResourceAsStream("/images/dashboard/exploration.png"));
            explorationTabImage.setImage(explorationImage);
            explorationTabImage.setFitWidth(60);
            explorationTabImage.setFitHeight(60);
            explorationTabImage.setPreserveRatio(true);
            explorationTabImage.setSmooth(true);

            Image colonisationTabIcon = loadColonisationTabIcon();
            colonisationTabImage.setImage(colonisationTabIcon);
            colonisationTabImage.setFitWidth(60);
            colonisationTabImage.setFitHeight(60);
            colonisationTabImage.setPreserveRatio(true);
            colonisationTabImage.setSmooth(true);
            
            // Configurer les effets élégants
            setupEliteTabEffects();
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des images des onglets: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Configure les effets simples et cohérents pour les onglets
     */
    private void setupEliteTabEffects() {
        // Ajouter les classes CSS de base
        missionsTabImage.getStyleClass().add("tab-image");
        miningTabImage.getStyleClass().add("tab-image");
        explorationTabImage.getStyleClass().add("tab-image");
        colonisationTabImage.getStyleClass().add("tab-image");
        
        // Gestion simple de la sélection des onglets
        setupSimpleTabSelection();
    }

    private static Image loadColonisationTabIcon() {
        var colonisation = DashboardController.class.getResourceAsStream("/images/dashboard/colonisation-tab.png");
        if (colonisation != null) {
            return new Image(colonisation);
        }
        var fleet = DashboardController.class.getResourceAsStream("/images/fleet.png");
        if (fleet != null) {
            return new Image(fleet);
        }
        var exploration = DashboardController.class.getResourceAsStream("/images/dashboard/exploration.png");
        if (exploration != null) {
            return new Image(exploration);
        }
        return new Image(DashboardController.class.getResourceAsStream("/images/dashboard/elitewarboard.png"));
    }
    
    /**
     * Configure la gestion simple de la sélection des onglets
     */
    private void setupSimpleTabSelection() {
        // Écouter les changements de sélection des onglets
        mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            // Réinitialiser tous les styles
            missionsTabImage.getStyleClass().remove("tab-image-selected");
            miningTabImage.getStyleClass().remove("tab-image-selected");
            explorationTabImage.getStyleClass().remove("tab-image-selected");
            colonisationTabImage.getStyleClass().remove("tab-image-selected");
            
            if (newTab == missionsTab) {
                // Onglet Missions sélectionné
                missionsTabImage.getStyleClass().add("tab-image-selected");
                System.out.println("✅ Missions sélectionné");
            } else if (newTab == miningTab) {
                // Onglet Mining sélectionné
                miningTabImage.getStyleClass().add("tab-image-selected");
                System.out.println("✅ Mining sélectionné");
            } else if (newTab == explorationTab) {
                // Onglet Exploration sélectionné
                explorationTabImage.getStyleClass().add("tab-image-selected");
                System.out.println("✅ Exploration sélectionné");
            } else if (newTab == colonisationTab) {
                colonisationTabImage.getStyleClass().add("tab-image-selected");
                System.out.println("✅ Colonisation sélectionné");
            }
            refreshGlobalModuleTutorialBar();
        });
        
        // Désactiver la fermeture des onglets
        missionsTab.setClosable(false);
        miningTab.setClosable(false);
        explorationTab.setClosable(false);
        colonisationTab.setClosable(false);
        
        // Sélectionner l'onglet Missions par défaut
        mainTabPane.getSelectionModel().select(missionsTab);

        // Appliquer immédiatement le style "onglet cliqué" à l'image Missions
        missionsTabImage.getStyleClass().add("tab-image-selected");
    }

    private void updateTranslations(){
        appTitleLabel.setText(localizationService.getString("header.app.title"));
        appSubtitleLabel.setText(localizationService.getString("header.app.subtitle"));
        if (primaryWindowChromeSupport != null) {
            primaryWindowChromeSupport.refreshLocalizedStrings();
        }
        updateStatusLabel();
        if (donateButtonImage != null) {
            donateButtonImage.setPickOnBounds(true);
            donateButtonImage.setPreserveRatio(true);
            Tooltip.install(donateButtonImage, new Tooltip(localizationService.getString("config.donate.button")));
        }
        if (colonisationTab != null) {
            colonisationTab.setTooltip(new Tooltip(localizationService.getString("colonisation.tab.tooltip")));
        }
        
        // Mettre à jour les traductions des labels commander/system/station
        if (commanderHeaderLabel != null) {
            commanderHeaderLabel.setText(localizationService.getString("footer.commander"));
        }
        if (systemHeaderLabel != null) {
            systemHeaderLabel.setText(localizationService.getString("footer.system"));
        }
        if (stationHeaderLabel != null) {
            stationHeaderLabel.setText(localizationService.getString("footer.station"));
        }
        refreshGlobalModuleTutorialBar();
    }
    @Override
    public void onBatchStart(){
        statusLabel.styleProperty().unbind();
        // Détacher les bindings des labels commander/system/station
        if (stationLabel != null) {
            stationLabel.textProperty().unbind();
        }
        if (systemLabel != null) {
            systemLabel.textProperty().unbind();
        }
        if (commanderLabel != null) {
            commanderLabel.textProperty().unbind();
        }
    }

    @Override
    public void onBatchEnd() {
        CommanderStatus.getInstance().syncOnlineToComponent();
        updateStatusLabel();
        // Binding conditionnel pour la couleur du statut
        statusLabel.styleProperty().bind(javafx.beans.binding.Bindings.when(commanderStatusComponent
                        .getIsOnline()).then("-fx-text-fill: #00ff00;") // Vert si en ligne
                .otherwise("-fx-text-fill: #ff0000;") // Rouge si hors ligne
        );
        
        // Attacher les bindings des labels commander/system/station
        if (stationLabel != null) {
            stationLabel.textProperty().bind(commanderStatusComponent.getCurrentStationName());
        }
        if (systemLabel != null) {
            systemLabel.textProperty().bind(commanderStatusComponent.getCurrentStarSystem());
        }
        if (commanderLabel != null) {
            commanderLabel.textProperty().bind(commanderStatusComponent.getCommanderName());
        }

        // Après {@code refreshAllUI()} dans le même pulse, ou tâches {@code runLater} restantes du replay.
        Platform.runLater(() -> {
            CommanderStatus.getInstance().syncOnlineToComponent();
            updateStatusLabel();
        });
    }
    
    /**
     * Gère le clic sur le label du système pour copier le nom dans le presse-papier
     */
    @FXML
    private void onSystemLabelClicked(MouseEvent event) {
        if (systemLabel == null) return;
        String systemName = systemLabel.getText();
        if (systemName != null && !systemName.isEmpty() && !systemName.equals("[NON IDENTIFIÉ]")) {
            copyClipboardManager.copyToClipboard(systemName);
            Stage stage = (Stage) systemLabel.getScene().getWindow();
            popupManager.showPopup(localizationService.getString("system.copied"), event.getSceneX(), event.getSceneY(), stage);
        }
    }

    private void updateStatusLabel() {
        if (commanderStatusComponent.getIsOnline().get()) {
            statusLabel.setText(localizationService.getString("commander.online"));
        } else {
            statusLabel.setText(localizationService.getString("commander.offline"));
        }
    }

    private void loadDonateButtonImage() {
        try {
            Image donateImage = new Image(
                    getClass().getResourceAsStream("/images/donate.png"),
                    0,
                    48,
                    true,
                    true
            );
            donateButtonImage.setImage(donateImage);
            donateButtonImage.setFitHeight(48);
            donateButtonImage.setPreserveRatio(true);
            donateButtonImage.setSmooth(true);
            donateButtonImage.setCache(true);
            donateButtonImage.setCacheHint(CacheHint.QUALITY);
        } catch (Exception e) {
            System.err.println("Erreur chargement image donate: " + e.getMessage());
        }
    }

    @FXML
    private void openDonateLink(MouseEvent event) {
        try {
            URI uri = new URI("https://www.paypal.com/donate/?hosted_button_id=2GSWMTWB4SHA2");
            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(uri);
            } else {
                System.err.println("Impossible d'ouvrir le navigateur pour le lien de don");
            }
        } catch (Exception e) {
            System.err.println("Erreur ouverture lien donate: " + e.getMessage());
        }
    }

    @FXML
    private void openConfigDialog() {
        Stage primaryStage = (Stage) configButton.getScene().getWindow();

        DialogComponent dialog = new DialogComponent("/fxml/combat/config-dialog.fxml", "/css/elite-theme.css", "Configuration", 780, 640);

        dialog.init(primaryStage);
        dialog.showAndWait();
    }

    @FXML
    private void onWindowMinimize() {
        if (primaryWindowChromeSupport != null) {
            primaryWindowChromeSupport.minimize();
        }
    }

    @FXML
    private void onWindowMaxRestoreToggle() {
        if (primaryWindowChromeSupport != null) {
            primaryWindowChromeSupport.toggleMaximized();
        }
    }

    @FXML
    private void onWindowClose() {
        if (primaryWindowChromeSupport != null) {
            primaryWindowChromeSupport.close();
        }
    }

    @Override
    public void refreshUI() {
        updateStatusLabel();
    }
}
