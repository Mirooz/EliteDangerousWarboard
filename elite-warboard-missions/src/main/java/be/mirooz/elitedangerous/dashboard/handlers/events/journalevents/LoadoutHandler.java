package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.commander.CommanderShip;
import be.mirooz.elitedangerous.dashboard.model.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.model.events.Loadout;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class LoadoutHandler implements JournalEventHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final CommanderStatus commanderStatus;

    public LoadoutHandler() {
        this.commanderStatus = CommanderStatus.getInstance();
    }

    @Override
    public String getEventType() {
        return "Loadout";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            Loadout loadout = parseLoadout(jsonNode);

            CommanderShip.ShipCargo oldCargo =
                    commanderStatus.getShip() != null ? commanderStatus.getShip().getShipCargo() : null;

            CommanderShip newShip = CommanderShip.builder()
                    .ship(loadout.getShip())
                    .shipCargo(oldCargo != null
                            ? oldCargo.copy()
                            : new CommanderShip.ShipCargo())
                    .maxRange(loadout.getMaxJumpRange())
                    .maxCapacity(loadout.getCargoCapacity())
                    .build();
            commanderStatus.setShip(newShip);

            System.out.printf("New commander ship parsed: %s (%d cargo capacity)%n",
                    newShip.getShip(), newShip.getMaxCapacity());

        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de Loadout: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Loadout parseLoadout(JsonNode jsonNode) {
        Loadout loadout = new Loadout();

        // Méthode utilitaire locale pour lire une valeur texte en toute sécurité
        Function<String, String> getText = field -> jsonNode.has(field) && !jsonNode.get(field).isNull() ? jsonNode.get(field).asText() : null;
        Function<String, Integer> getInt = field -> jsonNode.has(field) && !jsonNode.get(field).isNull() ? jsonNode.get(field).asInt() : null;
        Function<String, Long> getLong = field -> jsonNode.has(field) && !jsonNode.get(field).isNull() ? jsonNode.get(field).asLong() : null;
        Function<String, Double> getDouble = field -> jsonNode.has(field) && !jsonNode.get(field).isNull() ? jsonNode.get(field).asDouble() : null;

        // Parse basic ship information
        loadout.setTimestamp(getText.apply("timestamp"));
        loadout.setEvent(getText.apply("event"));
        loadout.setShip(getText.apply("Ship"));
        if (getInt.apply("ShipID") != null) loadout.setShipID(getInt.apply("ShipID"));
        loadout.setShipName(getText.apply("ShipName"));
        loadout.setShipIdent(getText.apply("ShipIdent"));
        if (getLong.apply("HullValue") != null) loadout.setHullValue(getLong.apply("HullValue"));
        if (getLong.apply("ModulesValue") != null) loadout.setModulesValue(getLong.apply("ModulesValue"));
        if (getDouble.apply("HullHealth") != null) loadout.setHullHealth(getDouble.apply("HullHealth"));
        if (getDouble.apply("UnladenMass") != null) loadout.setUnladenMass(getDouble.apply("UnladenMass"));
        if (getInt.apply("CargoCapacity") != null) loadout.setCargoCapacity(getInt.apply("CargoCapacity"));
        if (getDouble.apply("MaxJumpRange") != null) loadout.setMaxJumpRange(getDouble.apply("MaxJumpRange"));
        if (getLong.apply("Rebuy") != null) loadout.setRebuy(getLong.apply("Rebuy"));

        // Parse fuel capacity
        if (jsonNode.has("FuelCapacity") && !jsonNode.get("FuelCapacity").isNull()) {
            JsonNode fuelCapacityNode = jsonNode.get("FuelCapacity");
            Loadout.FuelCapacity fuelCapacity = new Loadout.FuelCapacity();

            if (fuelCapacityNode.has("Main") && !fuelCapacityNode.get("Main").isNull()) {
                fuelCapacity.setMain(fuelCapacityNode.get("Main").asDouble());
            }
            if (fuelCapacityNode.has("Reserve") && !fuelCapacityNode.get("Reserve").isNull()) {
                fuelCapacity.setReserve(fuelCapacityNode.get("Reserve").asDouble());
            }

            loadout.setFuelCapacity(fuelCapacity);
        }

        // Parse modules
        if (jsonNode.has("Modules") && jsonNode.get("Modules").isArray()) {
            JsonNode modulesNode = jsonNode.get("Modules");
            List<Loadout.Module> modules = new ArrayList<>();

            for (JsonNode moduleNode : modulesNode) {
                Loadout.Module module = parseModule(moduleNode);
                modules.add(module);
            }

            loadout.setModules(modules);
        }

        return loadout;
    }


    private Loadout.Module parseModule(JsonNode moduleNode) {
        Loadout.Module module = new Loadout.Module();

        // Fonctions utilitaires locales pour éviter la répétition
        Function<String, String> getText = field -> moduleNode.has(field) && !moduleNode.get(field).isNull() ? moduleNode.get(field).asText() : null;
        Function<String, Integer> getInt = field -> moduleNode.has(field) && !moduleNode.get(field).isNull() ? moduleNode.get(field).asInt() : null;
        Function<String, Long> getLong = field -> moduleNode.has(field) && !moduleNode.get(field).isNull() ? moduleNode.get(field).asLong() : null;
        Function<String, Double> getDouble = field -> moduleNode.has(field) && !moduleNode.get(field).isNull() ? moduleNode.get(field).asDouble() : null;
        Function<String, Boolean> getBoolean = field -> moduleNode.has(field) && !moduleNode.get(field).isNull() && moduleNode.get(field).asBoolean();

        // Champs obligatoires ou fréquents
        module.setSlot(getText.apply("Slot"));
        module.setItem(getText.apply("Item"));
        module.setOn(getBoolean.apply("On"));
        if (getInt.apply("Priority") != null) module.setPriority(getInt.apply("Priority"));

        // Champs optionnels
        if (getInt.apply("AmmoInClip") != null) module.setAmmoInClip(getInt.apply("AmmoInClip"));
        if (getInt.apply("AmmoInHopper") != null) module.setAmmoInHopper(getInt.apply("AmmoInHopper"));
        if (getDouble.apply("Health") != null) module.setHealth(getDouble.apply("Health"));
        if (getLong.apply("Value") != null) module.setValue(getLong.apply("Value"));

        // Parse engineering modifications if present
        if (moduleNode.has("Engineering") && !moduleNode.get("Engineering").isNull()) {
            Loadout.Engineering engineering = parseEngineering(moduleNode.get("Engineering"));
            module.setEngineering(engineering);
        }

        return module;
    }

    private Loadout.Engineering parseEngineering(JsonNode engineeringNode) {
        Loadout.Engineering engineering = new Loadout.Engineering();

        // Fonctions utilitaires locales
        Function<String, String> getText = field -> engineeringNode.has(field) && !engineeringNode.get(field).isNull() ? engineeringNode.get(field).asText() : null;
        Function<String, Integer> getInt = field -> engineeringNode.has(field) && !engineeringNode.get(field).isNull() ? engineeringNode.get(field).asInt() : null;
        Function<String, Double> getDouble = field -> engineeringNode.has(field) && !engineeringNode.get(field).isNull() ? engineeringNode.get(field).asDouble() : null;

        // Champs principaux
        engineering.setEngineer(getText.apply("Engineer"));
        if (getInt.apply("EngineerID") != null) engineering.setEngineerID(getInt.apply("EngineerID"));
        if (getInt.apply("BlueprintID") != null) engineering.setBlueprintID(getInt.apply("BlueprintID"));
        engineering.setBlueprintName(getText.apply("BlueprintName"));
        if (getInt.apply("Level") != null) engineering.setLevel(getInt.apply("Level"));
        if (getDouble.apply("Quality") != null) engineering.setQuality(getDouble.apply("Quality"));

        // Parse modifiers
        if (engineeringNode.has("Modifiers") && engineeringNode.get("Modifiers").isArray()) {
            JsonNode modifiersNode = engineeringNode.get("Modifiers");
            List<Loadout.Modifier> modifiers = new ArrayList<>();

            for (JsonNode modifierNode : modifiersNode) {
                Loadout.Modifier modifier = new Loadout.Modifier();

                if (modifierNode.has("Label") && !modifierNode.get("Label").isNull()) {
                    modifier.setLabel(modifierNode.get("Label").asText());
                }
                if (modifierNode.has("Value") && !modifierNode.get("Value").isNull()) {
                    modifier.setValue(modifierNode.get("Value").asDouble());
                }
                if (modifierNode.has("OriginalValue") && !modifierNode.get("OriginalValue").isNull()) {
                    modifier.setOriginalValue(modifierNode.get("OriginalValue").asDouble());
                }
                if (modifierNode.has("LessIsGood") && !modifierNode.get("LessIsGood").isNull()) {
                    modifier.setLessIsGood(modifierNode.get("LessIsGood").asInt());
                }

                modifiers.add(modifier);
            }

            engineering.setModifiers(modifiers);
        }

        return engineering;
    }

}
