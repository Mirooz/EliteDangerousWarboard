package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.CargoFile;
import be.mirooz.elitedangerous.dashboard.model.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.model.events.Cargo;
import be.mirooz.elitedangerous.dashboard.service.journal.JournalService;
import be.mirooz.elitedangerous.lib.inara.model.commodities.ICommodityFactory;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class CargoHandler implements JournalEventHandler {

    JournalService journalService;
    CommanderStatus commanderStatus = CommanderStatus.getInstance();

    @Override
    public String getEventType() {
        this.journalService = JournalService.getInstance();
        return "Cargo";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            Cargo cargo = parseCargo(jsonNode);
            System.out.println(commanderStatus.getShip().getShipCargo().getCommodities());
            if (cargo.getCount()==0 && "Ship".equals(cargo.getVessel())){
                commanderStatus.getShip().resetCargo();
            }
           // CargoFile cargoFile = journalService.readCargoFile();
           // commanderStatus.getShip().resetCargo();
           // commanderStatus.getShip().setCurrentUsed(cargoFile.getCount());
//            for (CargoFile.CargoItem item : cargoFile.getInventory()) {
//                ICommodityFactory.of(item.getName())
//                        .ifPresent(commodity -> commanderStatus.getShip().addCommodity(commodity, item.getCount()));
//            }
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de Cargo: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private Cargo parseCargo(JsonNode jsonNode) {
        Cargo cargo = new Cargo();

        // Fonctions utilitaires locales
        Function<String, String> getText = field -> jsonNode.has(field) && !jsonNode.get(field).isNull() ? jsonNode.get(field).asText() : null;
        Function<String, Integer> getInt = field -> jsonNode.has(field) && !jsonNode.get(field).isNull() ? jsonNode.get(field).asInt() : null;

        // Champs de base
        cargo.setTimestamp(getText.apply("timestamp"));
        cargo.setEvent(getText.apply("event"));
        cargo.setVessel(getText.apply("Vessel"));
        if (getInt.apply("Count") != null) cargo.setCount(getInt.apply("Count"));

        // Parse inventory si présent
        if (jsonNode.has("Inventory") && jsonNode.get("Inventory").isArray()) {
            JsonNode inventoryNode = jsonNode.get("Inventory");
            List<Cargo.Inventory> inventory = new ArrayList<>();

            for (JsonNode itemNode : inventoryNode) {
                Cargo.Inventory item = parseInventoryItem(itemNode);
                inventory.add(item);
            }

            cargo.setInventory(inventory);
        }

        return cargo;
    }

    private Cargo.Inventory parseInventoryItem(JsonNode itemNode) {
        Cargo.Inventory item = new Cargo.Inventory();

        // Fonctions utilitaires locales
        Function<String, String> getText = field -> itemNode.has(field) && !itemNode.get(field).isNull() ? itemNode.get(field).asText() : null;
        Function<String, Integer> getInt = field -> itemNode.has(field) && !itemNode.get(field).isNull() ? itemNode.get(field).asInt() : null;

        // Champs
        item.setName(getText.apply("Name"));
        item.setNameLocalised(getText.apply("Name_Localised"));
        if (getInt.apply("Count") != null) item.setCount(getInt.apply("Count"));
        if (getInt.apply("Stolen") != null) item.setStolen(getInt.apply("Stolen"));

        return item;
    }

}

