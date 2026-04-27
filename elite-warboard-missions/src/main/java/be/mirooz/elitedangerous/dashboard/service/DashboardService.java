package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.dashboard.view.common.IBatchListener;
import be.mirooz.elitedangerous.dashboard.view.common.context.DashboardContext;
import be.mirooz.elitedangerous.dashboard.view.common.managers.UIManager;
import be.mirooz.elitedangerous.dashboard.model.registries.combat.DestroyedShipsRegistery;
import be.mirooz.elitedangerous.dashboard.model.registries.combat.MissionsRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.combat.ShipTargetRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.ExplorationDataSaleRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.OrganicDataSaleRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.PlaneteRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.SystemVisitedRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.mining.MiningStatRegistry;
import be.mirooz.elitedangerous.dashboard.service.journal.JournalService;
import be.mirooz.elitedangerous.dashboard.service.journal.watcher.JournalTailService;
import be.mirooz.elitedangerous.dashboard.service.journal.watcher.JournalWatcherService;
import javafx.application.Platform;

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
                MiningService.getInstance().clearAllProspectors();
                MiningStatRegistry.getInstance().clearAllStats();
                CombatMissionHistoryService.getInstance().clear();
                //EXPLo
                OrganicDataSaleRegistry.getInstance().clear();
                PlaneteRegistry.getInstance().clear();
                SystemVisitedRegistry.getInstance().clear();
                ExplorationDataSaleRegistry.getInstance().clearAll();
                JournalWatcherService.getInstance().stop();
                journalService.getMissionsFromLastWeek();
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                // Un seul pulse FX : d’abord tous les onBatchEnd (liaisons header, listes, etc.),
                // puis rafraîchissement global des panneaux enregistrés (UIManager).
                Platform.runLater(() -> {
                    for (IBatchListener l : listeners) {
                        l.onBatchEnd();
                    }
                    UIManager.getInstance().refreshAllUI();
                });
            }
        }).start();
    }

}
