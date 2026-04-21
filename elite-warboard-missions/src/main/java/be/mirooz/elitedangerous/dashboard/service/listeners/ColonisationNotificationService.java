package be.mirooz.elitedangerous.dashboard.service.listeners;

import be.mirooz.elitedangerous.dashboard.view.common.context.DashboardContext;
import javafx.application.Platform;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Notifie l’UI colonisation (chantier, fleet carrier, ressources) après des événements journal ou CAPI pertinents.
 * Pendant {@link DashboardContext#isBatchLoading()}, les appels sont ignorés ; un rafraîchissement global est déclenché
 * à la fin du replay journal (voir {@link be.mirooz.elitedangerous.dashboard.service.journal.JournalService}).
 */
public final class ColonisationNotificationService {

    private static ColonisationNotificationService instance;

    private final List<ColonisationDataListener> listeners = new CopyOnWriteArrayList<>();
    private final DashboardContext dashboardContext = DashboardContext.getInstance();

    private ColonisationNotificationService() {
    }

    public static synchronized ColonisationNotificationService getInstance() {
        if (instance == null) {
            instance = new ColonisationNotificationService();
        }
        return instance;
    }

    public void addListener(ColonisationDataListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(ColonisationDataListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notifie les abonnés sur le thread JavaFX (regroupe un seul {@code runLater} si appel hors FX).
     */
    public void notifyColonisationDataChanged() {
        if (dashboardContext.isBatchLoading()) {
            return;
        }
        Runnable run = () -> {
            for (ColonisationDataListener listener : listeners) {
                try {
                    listener.onColonisationDataChanged();
                } catch (Exception e) {
                    System.err.println("ColonisationNotificationService: " + e.getMessage());
                }
            }
        };
        if (Platform.isFxApplicationThread()) {
            run.run();
        } else {
            Platform.runLater(run);
        }
    }

    @FunctionalInterface
    public interface ColonisationDataListener {
        void onColonisationDataChanged();
    }
}
