package be.mirooz.elitedangerous.dashboard.controller.ui.component;

import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.Mineral;
import be.mirooz.elitedangerous.dashboard.model.mining.MiningStat;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.service.MiningService;
import be.mirooz.elitedangerous.dashboard.service.MiningStatsService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Composant pour l'historique des sessions de minage
 * <p>
 * Ce composant gère :
 * - L'affichage des statistiques globales (sessions totales, durée, valeur)
 * - La liste des sessions de minage terminées
 * - Le tri par date (plus récent en premier)
 * - Les tooltips avec le détail des minéraux
 */
public class MiningHistoryComponent implements Initializable {

    // Services
    private final MiningService miningService = MiningService.getInstance();
    private final MiningStatsService miningStatsService = MiningStatsService.getInstance();
    private final LocalizationService localizationService = LocalizationService.getInstance();

    // Composants FXML
    @FXML
    private Label miningHistoryLabel;
    @FXML
    private Label totalSessionsLabel;
    @FXML
    private Label totalSessionsTitleLabel;
    @FXML
    private Label totalDurationLabel;
    @FXML
    private Label totalDurationTitleLabel;
    @FXML
    private Label totalValueLabel;
    @FXML
    private Label totalValueTitleLabel;
    @FXML
    private ScrollPane miningHistoryScrollPane;
    @FXML
    private VBox miningHistoryList;

    // Callback pour notifier le parent des changements
    private Runnable onHistoryUpdated;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeMiningHistory();
        updateTranslations();
        
        // Écouter les changements de langue
        localizationService.addLanguageChangeListener(locale -> updateTranslations());
    }

    /**
     * Initialise l'historique des sessions de minage
     */
    private void initializeMiningHistory() {
        updateMiningHistory();
    }

    /**
     * Met à jour l'historique des sessions de minage
     */
    public void updateMiningHistory() {
        Platform.runLater(() -> {
            // Mettre à jour les statistiques globales
            MiningStatsService.MiningGlobalStats globalStats = miningStatsService.getGlobalStats();
            totalSessionsLabel.setText(String.valueOf(globalStats.getTotalSessions()));
            totalDurationLabel.setText(formatDuration(globalStats.getTotalDurationMinutes()));
            totalValueLabel.setText(miningService.formatPrice(globalStats.getTotalValue()) + " Cr");

            // Mettre à jour la liste des sessions
            updateMiningHistoryList();

            // Notifier le parent du changement
            if (onHistoryUpdated != null) {
                onHistoryUpdated.run();
            }
        });
    }

    /**
     * Met à jour la liste des sessions de minage
     */
    private void updateMiningHistoryList() {
        miningHistoryList.getChildren().clear();

        List<MiningStat> completedStats = miningStatsService.getCompletedMiningStats();

        if (completedStats.isEmpty()) {
            Label noSessionsLabel = new Label("Aucune session terminée");
            noSessionsLabel.getStyleClass().add("no-sessions-message");
            miningHistoryList.getChildren().add(noSessionsLabel);
            return;
        }

        // Créer une nouvelle liste mutable pour pouvoir trier
        List<MiningStat> sortedStats = new ArrayList<>(completedStats);

        // Trier par date de début (plus récent en premier)
        sortedStats.sort((a, b) -> b.getStartDate().compareTo(a.getStartDate()));

        for (MiningStat stat : sortedStats) {
            VBox sessionCard = createMiningSessionCard(stat);
            miningHistoryList.getChildren().add(sessionCard);
        }
    }

    /**
     * Crée une carte pour une session de minage
     */
    private VBox createMiningSessionCard(MiningStat stat) {
        VBox card = new VBox(5);
        if (stat.isActive()) {
            card.getStyleClass().addAll("mining-session-card", "mining-session-card-current");
        } else {
            card.getStyleClass().add("mining-session-card");
        }

        // En-tête avec système et anneau
        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label systemLabel = new Label(stat.getSystemName());
        systemLabel.getStyleClass().add("session-system");

        Label ringLabel = new Label(shortenRingName(stat.getRingName(), stat.getSystemName()));
        ringLabel.getStyleClass().add("session-ring");

        // Ajouter un indicateur "CURRENT" si la session est active
        if (stat.isActive()) {
            Label currentLabel = new Label(getTranslation("mining.current_session"));
            currentLabel.getStyleClass().add("session-current");
            header.getChildren().addAll(systemLabel, ringLabel, currentLabel);
        } else {
            header.getChildren().addAll(systemLabel, ringLabel);
        }

        // Informations de la session
        VBox info = new VBox(3);

        // Durée
        Label durationLabel = new Label("Durée: " + formatDuration(stat.getDurationInMinutes()));
        durationLabel.getStyleClass().add("session-info");

        // Nombre de minéraux
        int totalMinerals = stat.getRefinedMinerals().size();
        Label mineralsLabel = new Label("Minéraux: " + totalMinerals);
        mineralsLabel.getStyleClass().add("session-info");

        // Valeur
        Label valueLabel = new Label("Valeur: " + miningService.formatPrice(stat.getTotalValue()) + " Cr");
        valueLabel.getStyleClass().add("session-value");

        info.getChildren().addAll(durationLabel, mineralsLabel, valueLabel);

        // Date
        Label dateLabel = new Label(formatDate(stat.getStartDate()));
        dateLabel.getStyleClass().add("session-date");

        card.getChildren().addAll(header, info, dateLabel);

        // Ajouter un tooltip avec le détail des minéraux
        Tooltip tooltip = createMineralsTooltip(stat);
        Tooltip.install(card, tooltip);

        return card;
    }

    /**
     * Crée un tooltip avec le détail des minéraux raffinés
     */
    private Tooltip createMineralsTooltip(MiningStat stat) {
        StringBuilder tooltipText = new StringBuilder();
        tooltipText.append("Minéraux raffinés:\n");

        Map<Mineral, Integer> minerals = stat.getTotalRefinedMinerals();

        if (minerals.isEmpty()) {
            tooltipText.append("Aucun minéral raffiné");
        } else {
            minerals.entrySet().stream()
                    .sorted(Map.Entry.<Mineral, Integer>comparingByValue().reversed())
                    .forEach(entry -> {
                        tooltipText.append("• ")
                                .append(entry.getKey().getVisibleName())
                                .append(": ")
                                .append(entry.getValue())
                                .append(" unités\n");
                    });

            // Supprimer le dernier \n
            if (tooltipText.length() > 0) {
                tooltipText.setLength(tooltipText.length() - 1);
            }
        }

        Tooltip tooltip = new TooltipComponent(tooltipText.toString());
        tooltip.getStyleClass().add("mining-session-tooltip");
        tooltip.setShowDelay(javafx.util.Duration.millis(500));
        tooltip.setHideDelay(javafx.util.Duration.millis(200));

        return tooltip;
    }

    /**
     * Formate une durée en minutes en heures et minutes
     */
    private String formatDuration(long minutes) {
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        return String.format("%dh %dm", hours, remainingMinutes);
    }

    /**
     * Formate une date pour l'affichage
     */
    private String formatDate(java.time.LocalDateTime date) {
        return date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    /**
     * Raccourcit le nom de l'anneau en supprimant la répétition du nom du système
     */
    private String shortenRingName(String ringName, String systemName) {
        if (ringName == null || systemName == null) {
            return ringName;
        }

        // Si le nom de l'anneau commence par le nom du système, le supprimer
        if (ringName.startsWith(systemName)) {
            String shortened = ringName.substring(systemName.length()).trim();
            // Supprimer les espaces en début et les caractères de séparation comme " - " ou " "
            if (shortened.startsWith(" - ") || shortened.startsWith(" ")) {
                shortened = shortened.replaceFirst("^\\s*-?\\s*", "");
            }
            return shortened.isEmpty() ? ringName : shortened;
        }

        return ringName;
    }

    /**
     * Met à jour toutes les traductions de l'interface
     */
    private void updateTranslations() {
        if (miningHistoryLabel != null) {
            miningHistoryLabel.setText(getTranslation("mining.history_sessions"));
        }
        if (totalSessionsTitleLabel != null) {
            totalSessionsTitleLabel.setText(getTranslation("mining.total_sessions"));
        }
        if (totalDurationTitleLabel != null) {
            totalDurationTitleLabel.setText(getTranslation("mining.total_duration"));
        }
        if (totalValueTitleLabel != null) {
            totalValueTitleLabel.setText(getTranslation("mining.total_value"));
        }
    }

    /**
     * Récupère une traduction depuis le LocalizationService
     */
    private String getTranslation(String key) {
        return localizationService.getString(key);
    }

    // Getters et setters
    public void setOnHistoryUpdated(Runnable onHistoryUpdated) {
        this.onHistoryUpdated = onHistoryUpdated;
    }

    /**
     * Retourne les statistiques globales de minage
     */
    public MiningStatsService.MiningGlobalStats getGlobalStats() {
        return miningStatsService.getGlobalStats();
    }

    /**
     * Retourne la liste des sessions terminées
     */
    public List<MiningStat> getCompletedSessions() {
        return miningStatsService.getCompletedMiningStats();
    }

    /**
     * Force le rafraîchissement de l'affichage
     */
    public void refresh() {
        updateMiningHistory();
    }

    // Getters pour accéder aux composants depuis l'extérieur
    public Label getTotalSessionsLabel() {
        return totalSessionsLabel;
    }

    public Label getTotalDurationLabel() {
        return totalDurationLabel;
    }

    public Label getTotalValueLabel() {
        return totalValueLabel;
    }

    public VBox getMiningHistoryList() {
        return miningHistoryList;
    }
}
