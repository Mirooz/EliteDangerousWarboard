package be.mirooz.elitedangerous.dashboard.service.listeners;

import be.mirooz.elitedangerous.commons.lib.models.commodities.ICommodityFactory;
import be.mirooz.elitedangerous.dashboard.view.common.context.DashboardContext;
import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderShip;
import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderStatus;
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

            // Mapper chaque item de l'inventaire (champ Name du Cargo.json = identifiant jeu, aligné sur le registre)
            if (cargoData.getInventory() != null) {
                for (Cargo.Inventory item : cargoData.getInventory()) {
                    if (item.getCount() == null || item.getCount() <= 0) {
                        continue;
                    }
                    String rawName = item.getName();
                    if (rawName == null || rawName.isBlank()) {
                        System.out.printf(
                                "⚠️ Cargo.json: ligne inventaire sans Name (localised=%s) x%d — ignorée%n",
                                item.getNameLocalised() != null ? item.getNameLocalised() : "?",
                                item.getCount());
                        continue;
                    }
                    ICommodityFactory.ofByCargoJson(rawName)
                            .ifPresentOrElse(
                                    commodity -> {
                                        commanderStatus.getShip().getJsonShipCargo().addCommodity(commodity, item.getNameLocalised(), item.getCount());
                                        System.out.printf("📦 Cargo mappé: %s x%d%n", commodity.getVisibleName(), item.getCount());
                                    },
                                    () -> System.out.printf(
                                            "⚠️ Cargo.json: commodité non reconnue Name=%s (localised=%s) x%d — absente du registre / enums%n",
                                            rawName,
                                            item.getNameLocalised() != null ? item.getNameLocalised() : "?",
                                            item.getCount()));
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
