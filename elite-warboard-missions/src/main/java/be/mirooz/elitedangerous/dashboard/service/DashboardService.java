package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.dashboard.controller.IBatchListener;
import be.mirooz.elitedangerous.dashboard.controller.ui.context.DashboardContext;
import be.mirooz.elitedangerous.dashboard.model.commander.Mission;
import be.mirooz.elitedangerous.dashboard.model.enums.MissionStatus;
import be.mirooz.elitedangerous.dashboard.model.enums.MissionType;
import be.mirooz.elitedangerous.dashboard.model.exploration.PlaneteDetail;
import be.mirooz.elitedangerous.dashboard.model.registries.*;
import be.mirooz.elitedangerous.dashboard.service.journal.JournalService;
import be.mirooz.elitedangerous.dashboard.service.journal.watcher.JournalTailService;
import be.mirooz.elitedangerous.dashboard.service.journal.watcher.JournalWatcherService;
import javafx.application.Platform;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


/**
 * Service pour gérer les missions Elite Dangerous
 */
public class DashboardService {
    private static final DashboardService INSTANCE = new DashboardService();

    private DashboardService() {
        this.journalService = JournalService.getInstance();
    }


    public void addBatchListener(IBatchListener controller) {
        listeners.add(controller);
    }

    public static DashboardService getInstance() {
        return INSTANCE;
    }

    private final List<IBatchListener> listeners = new ArrayList<>();
    private final JournalService journalService;

    /**
     * Récupère la liste des missions actives
     *
     * @return Liste des missions actives
     */
    public void initActiveMissions() {
        listeners.forEach(l -> Platform.runLater(l::onBatchStart));

        new Thread(() -> {
            try {
                MissionsRegistry.getInstance().clear();
                DestroyedShipsRegistery.getInstance().clearAll();
                ShipTargetRegistry.getInstance().clear();
                JournalTailService.getInstance().stop();
                ProspectedAsteroidRegistry.getInstance().clear();
                MiningStatRegistry.getInstance().clearAllStats();
                CombatMissionHistoryService.getInstance().clear();
                JournalWatcherService.getInstance().stop();
                journalService.getMissionsFromLastWeek();
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                listeners.forEach(l -> Platform.runLater(l::onBatchEnd));
                DashboardContext.getInstance().refreshUI();

            }
        }).start();
    }

}
