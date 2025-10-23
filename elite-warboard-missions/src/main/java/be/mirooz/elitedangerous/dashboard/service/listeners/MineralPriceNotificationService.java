package be.mirooz.elitedangerous.dashboard.service.listeners;

import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.Mineral;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service pour notifier les changements de prix des minéraux
 */
public class MineralPriceNotificationService {

    private static MineralPriceNotificationService instance;
    private final List<MineralPriceListener> listeners = new CopyOnWriteArrayList<>();

    private MineralPriceNotificationService() {
    }

    public static MineralPriceNotificationService getInstance() {
        if (instance == null) {
            instance = new MineralPriceNotificationService();
        }
        return instance;
    }

    /**
     * Ajoute un listener pour les changements de prix
     */
    public void addListener(MineralPriceListener listener) {
        listeners.add(listener);
    }

    /**
     * Supprime un listener
     */
    public void removeListener(MineralPriceListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notifie tous les listeners qu'un prix de minéral a changé
     */
    public void notifyPriceChanged(Mineral mineral, long oldPrice, long newPrice) {
        for (MineralPriceListener listener : listeners) {
            listener.onMineralPriceChanged(mineral, oldPrice, newPrice);
        }
    }

    /**
     * Interface pour écouter les changements de prix des minéraux
     */
    public interface MineralPriceListener {
        void onMineralPriceChanged(Mineral mineral, long oldPrice, long newPrice);
    }
}
