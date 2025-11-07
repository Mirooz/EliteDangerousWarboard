package be.mirooz.elitedangerous.dashboard.controller.ui.component.combat;

import be.mirooz.elitedangerous.dashboard.controller.IBatchListener;
import be.mirooz.elitedangerous.dashboard.controller.ui.component.TooltipComponent;
import be.mirooz.elitedangerous.dashboard.controller.ui.context.DashboardContext;
import be.mirooz.elitedangerous.dashboard.controller.ui.manager.CopyClipboardManager;
import be.mirooz.elitedangerous.dashboard.controller.ui.manager.PopupManager;
import be.mirooz.elitedangerous.dashboard.model.combat.CombatMissionStats;
import be.mirooz.elitedangerous.dashboard.model.enums.MissionType;
import be.mirooz.elitedangerous.dashboard.service.CombatMissionHistoryService;
import be.mirooz.elitedangerous.dashboard.service.DashboardService;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Composant pour afficher l'historique des missions de massacre complétées
 */
public class CombatMissionHistoryComponent implements Initializable, IBatchListener {

    // Services
    private final CombatMissionHistoryService historyService = CombatMissionHistoryService.getInstance();
    private final LocalizationService localizationService = LocalizationService.getInstance();
    private final CopyClipboardManager copyClipboardManager = CopyClipboardManager.getInstance();
    private final PopupManager popupManager = PopupManager.getInstance();

    // Composants FXML
    @FXML
    private Label historyTitleLabel;
    
    @FXML
    private VBox historyListBox;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateTranslations();
        DashboardService.getInstance().addBatchListener(this);
        // Écouter les changements de langue
        localizationService.addLanguageChangeListener(locale -> refreshHistory());

        localizationService.addLanguageChangeListener(locale -> updateTranslations());
    }

    @Override
    public void onBatchStart() {
        // Écouter les changements d'historique
        historyService.removeListeners();
    }
    @Override
    public void onBatchEnd() {
        refreshHistory();
        historyService.addListener(this::refreshHistory);
    }
    /**
     * Met à jour les traductions
     */
    private void updateTranslations() {
        if (historyTitleLabel != null) {
            historyTitleLabel.setText(getTranslation("combat.history.title"));
        }
    }
    
    /**
     * Rafraîchit l'affichage de l'historique
     */
    public void refreshHistory() {
        if (!DashboardContext.getInstance().isBatchLoading()) {
            Platform.runLater(() -> {
                if (historyListBox != null) {
                    updateHistory();
                }
            });
        }
    }
    
    /**
     * Met à jour la liste complète de l'historique
     */
    private void updateHistory() {
        historyListBox.getChildren().clear();
        
        List<CombatMissionStats> allStats = historyService.getAllStats();
        
        if (allStats.isEmpty()) {
            Label noData = new Label(getTranslation("combat.history.no_data"));
            noData.getStyleClass().add("history-no-data");
            historyListBox.getChildren().add(noData);
            return;
        }
        
        for (CombatMissionStats stats : allStats) {
            VBox card = createStatsCard(stats);
            historyListBox.getChildren().add(card);
        }
    }
    
    /**
     * Crée une carte de statistiques
     */
    private VBox createStatsCard(CombatMissionStats stats) {
        VBox card = new VBox(5);
        card.getStyleClass().add("history-card");
        card.setPadding(new Insets(10));
        
        // Catégorie (PIRATE ou CONFLIT) - centré
        Label categoryLabel;
        if (MissionType.MASSACRE.equals(stats.getMissionCategory())) {
            categoryLabel = new Label(localizationService.getString("target.pirate"));
            categoryLabel.getStyleClass().add("history-category-pirate");
        }
        else {
            categoryLabel = new Label(localizationService.getString("target.faction"));
            categoryLabel.getStyleClass().add("history-category-conflict");
        }
        categoryLabel.setAlignment(javafx.geometry.Pos.CENTER);
        categoryLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(categoryLabel, javafx.scene.layout.Priority.ALWAYS);
        
        // Système origine -> destination (clicables individuellement)
        HBox routeBox = new HBox(5);
        routeBox.setAlignment(Pos.CENTER_LEFT);
        
        // Système d'origine - cliquable
        Label originLabel = new Label(stats.getOriginSystem());
        originLabel.getStyleClass().addAll("history-system", "clickable-system-source");
        originLabel.setTooltip(new TooltipComponent(localizationService.getString("tooltip.origin_system") + ": " + stats.getOriginSystem()));
        originLabel.setOnMouseClicked(e -> onClickSystem(stats.getOriginSystem(), e));
        
        // Flèche
        Label arrowLabel = new Label("→");
        arrowLabel.getStyleClass().add("history-arrow");
        
        // Système destination - cliquable
        Label destLabel = new Label(stats.getDestinationSystem());
        destLabel.getStyleClass().addAll("history-system", "clickable-system-target");
        destLabel.setTooltip(new TooltipComponent(localizationService.getString("tooltip.destination_system") + ": " + stats.getDestinationSystem()));
        destLabel.setOnMouseClicked(e -> onClickSystem(stats.getDestinationSystem(), e));
        
        routeBox.getChildren().addAll(originLabel, arrowLabel, destLabel);
        
        // Stats
        HBox statsBox = new HBox(20);
        
        Label missionsLabel = new Label(getTranslation("combat.history.missions") + ": " + stats.getCompletedMissions());
        Label killsLabel = new Label(getTranslation("combat.history.kills") + ": " + stats.getTotalKills());
        Label rewardLabel = new Label(formatPrice(stats.getTotalReward()));
        
        missionsLabel.getStyleClass().add("history-stat");
        killsLabel.getStyleClass().add("history-stat");
        rewardLabel.getStyleClass().add("history-reward");
        
        statsBox.getChildren().addAll(missionsLabel, killsLabel, rewardLabel);
        
        // Date de dernière activité
        Label lastActivityLabel = new Label(getTranslation("combat.history.last") + ": " + formatLastActivity(stats.getLastCompleted()));
        lastActivityLabel.getStyleClass().add("history-last-activity");
        
        card.getChildren().addAll(categoryLabel, routeBox, statsBox, lastActivityLabel);
        
        return card;
    }
    
    /**
     * Formate la dernière activité (affiche la date)
     */
    private String formatLastActivity(java.time.LocalDateTime lastActivity) {
        if (lastActivity == null) {
            return "N/A";
        }
        
        // Formatter pour afficher: DD/MM HH:mm
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm");
        return lastActivity.format(formatter);
    }
    
    /**
     * Gère le clic sur un système
     */
    private void onClickSystem(String systemName, javafx.scene.input.MouseEvent event) {
        copyClipboardManager.copyToClipboard(systemName);
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        popupManager.showPopup(localizationService.getString("system.copied"), event.getSceneX(), event.getSceneY(), stage);
    }
    
    /**
     * Formate un prix avec points comme séparateurs
     */
    private String formatPrice(long price) {
        // Utiliser DecimalFormat avec points comme séparateurs
        java.text.DecimalFormatSymbols symbols = new java.text.DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        
        java.text.DecimalFormat df = new java.text.DecimalFormat("#,###", symbols);
        return df.format(price) + " Cr";
    }
    
    /**
     * Récupère une traduction
     */
    private String getTranslation(String key) {
        return localizationService.getString(key);
    }
}

