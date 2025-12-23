package be.mirooz.elitedangerous.dashboard.service.listeners;

import be.mirooz.elitedangerous.commons.lib.models.commodities.ICommodity;
import be.mirooz.elitedangerous.commons.lib.models.commodities.ICommodityFactory;
import be.mirooz.elitedangerous.dashboard.view.common.context.DashboardContext;
import be.mirooz.elitedangerous.dashboard.model.commander.CommanderShip;
import be.mirooz.elitedangerous.dashboard.model.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.model.events.Cargo;
import be.mirooz.elitedangerous.dashboard.service.journal.JournalService;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service pour notifier les changements de prix des minéraux
 */
public class CargoEventNotificationService {

    private static CargoEventNotificationService instance;
    private final List<CargoEventInterface> listeners = new CopyOnWriteArrayList<>();
    private final DashboardContext dashboardContext = DashboardContext.getInstance();

    private CargoEventNotificationService() {
    }

    public static CargoEventNotificationService getInstance() {
        if (instance == null) {
            instance = new CargoEventNotificationService();
        }
        return instance;
    }

    /**
     * Ajoute un listener pour les changements de prix
     */
    public void addListener(CargoEventInterface listener) {
        listeners.add(listener);
    }

    /**
     * Supprime un listener
     */
    public void removeListener(CargoEventInterface listener) {
        listeners.remove(listener);
    }

    /**
     * Notifie tous les listeners qu'un prix de minéral a changé
     */
    public void notifyCargoEvent() {
        if (!dashboardContext.isBatchLoading()) {
            // Mapper le cargo.json sur le CommanderShip.jsonShipCargo
            mapCargoJsonToCommanderShip();

            // Notifier tous les listeners
            for (CargoEventInterface listener : listeners) {
                listener.onReadCargoJson();
            }
        }
    }

    /**
     * Mappe le contenu du fichier cargo.json sur le CommanderShip.jsonShipCargo
     */
    private void mapCargoJsonToCommanderShip() {
        try {
            JournalService journalService = JournalService.getInstance();
            Cargo cargoData = journalService.readCargoFile();

            if (cargoData == null) {
                System.out.println("⚠️ Aucune donnée cargo trouvée dans cargo.json");
                return;
            }

            CommanderStatus commanderStatus = CommanderStatus.getInstance();
            if (commanderStatus.getShip() == null) {
                System.out.println("⚠️ Aucun vaisseau trouvé pour mapper le cargo");
                return;
            }
            commanderStatus.getShip().setJsonShipCargo(new CommanderShip.ShipCargo());

            // Mapper chaque item de l'inventaire
            if (cargoData.getInventory() != null) {
                for (Cargo.Inventory item : cargoData.getInventory()) {
                    if (item.getName() != null && item.getCount() != null && item.getCount() > 0) {
                        // Créer la commodité à partir du nom cargo JSON
                        ICommodityFactory.ofByCargoJson(item.getName())
                                .ifPresent(commodity -> {
                                    commanderStatus.getShip().getJsonShipCargo().addCommodity(commodity, item.getCount());
                                    System.out.printf("📦 Cargo mappé: %s x%d%n", commodity.getVisibleName(), item.getCount());
                                });
                    }
                }
            }
            // Mapper le count total
            commanderStatus.getShip().getJsonShipCargo().setCurrentUsed(cargoData.getCount() != null ? cargoData.getCount() : 0);

            System.out.printf("✅ Cargo.json mappé sur CommanderShip.jsonShipCargo: %d slots utilisés%n",
                    commanderStatus.getShip().getJsonShipCargo().getCurrentUsed());

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du mapping du cargo.json: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Interface pour écouter les changements de prix des minéraux
     */
    public interface CargoEventInterface {
        void onReadCargoJson();
    }
}
