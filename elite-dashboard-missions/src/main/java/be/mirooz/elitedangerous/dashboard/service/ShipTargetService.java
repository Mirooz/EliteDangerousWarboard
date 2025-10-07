package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.dashboard.model.ships.ShipTarget;
import be.mirooz.elitedangerous.dashboard.model.registries.ShipTargetRegistry;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collection;
import java.util.Optional;

public class ShipTargetService {

    private final ShipTargetRegistry shipTargetRegistry = ShipTargetRegistry.getInstance();
    private static final ShipTargetService INSTANCE = new ShipTargetService();

    // Registry interne
    private final ShipTargetRegistry registry = ShipTargetRegistry.getInstance();

    // Constructeur privé
    private ShipTargetService() {}

    // Accès global au singleton
    public static ShipTargetService getInstance() {
        return INSTANCE;
    }
    /**
     * Ajoute ou met à jour une cible dans le registre.
     * Si la limite est dépassée, la plus ancienne est automatiquement supprimée.
     */
    public void registerTarget(JsonNode jsonNode) {
        if (jsonNode == null || !"ShipTargeted".equals(jsonNode.path("event").asText())) {
            return;
        }

        String pilotNameLocalised = jsonNode.path("PilotName_Localised").asText(null);
        if (pilotNameLocalised == null) {
            return;
        }

        ShipTarget target = Optional.ofNullable(registry.get(pilotNameLocalised))
                .orElseGet(ShipTarget::new);

        target.setPilotNameLocalised(pilotNameLocalised);
        updateTargetFromJson(target, jsonNode);
    }
    public void commitCrimeToTarget(JsonNode jsonNode){

        String pilotNameLocalised = jsonNode.path("Victim").asText(null);
        if (pilotNameLocalised == null) {
            return;
        }
        ShipTarget target = Optional.ofNullable(registry.get(pilotNameLocalised))
                .orElseGet(ShipTarget::new);

        target.setPilotNameLocalised(pilotNameLocalised);
        updateCrimeCommited(target, jsonNode);
    }

    /**
     * Met à jour un ShipTarget existant à partir des champs présents dans le JSON.
     */
    private void updateTargetFromJson(ShipTarget target, JsonNode json) {

        if (json.hasNonNull("timestamp")) {
            target.setTimestamp(json.get("timestamp").asText());
        }

        setIfPresent(json, "TargetLocked", node -> target.setTargetLocked(node.asBoolean()));
        setIfPresent(json, "Ship", node -> target.setShip(node.asText()));
        setIfPresent(json, "Ship_Localised", node -> target.setShipLocalised(node.asText()));
        setIfPresent(json, "ScanStage", node -> target.setScanStage(node.asInt()));
        setIfPresent(json, "PilotName", node -> target.setPilotName(node.asText()));
        setIfPresent(json, "PilotRank", node -> target.setPilotRank(node.asText()));
        setIfPresent(json, "ShieldHealth", node -> target.setShieldHealth(node.asDouble()));
        setIfPresent(json, "HullHealth", node -> target.setHullHealth(node.asDouble()));
        setIfPresent(json, "Faction", node -> target.setFaction(node.asText()));
        setIfPresent(json, "LegalStatus", node -> target.setLegalStatus(node.asText()));
        setIfPresent(json, "Bounty", node -> target.setBounty(node.asLong()));

        registry.put(target);
    }
    private void updateCrimeCommited(ShipTarget target, JsonNode json) {

        if (json.hasNonNull("timestamp")) {
            target.setTimestamp(json.get("timestamp").asText());
        }

        target.setCrimeCommitted(true);
        registry.put(target);
    }

    /**
     * Petit utilitaire pour éviter de répéter les if(json.hasNonNull(...))
     */
    private void setIfPresent(JsonNode json, String fieldName, java.util.function.Consumer<JsonNode> setter) {
        if (json.hasNonNull(fieldName)) {
            setter.accept(json.get(fieldName));
        }
    }

    /**
     * Récupère une cible par son nom localisé.
     */
    public ShipTarget getTarget(String pilotNameLocalised) {
        if (pilotNameLocalised != null && !pilotNameLocalised.isEmpty())
            return shipTargetRegistry.get(pilotNameLocalised);
        return null;
    }

    /**
     * Retourne toutes les cibles actuellement enregistrées.
     */
    public Collection<ShipTarget> getAllTargets() {
        return shipTargetRegistry.getAll().values();
    }

    /**
     * Supprime toutes les cibles.
     */
    public void clearAllTargets() {
        shipTargetRegistry.clear();
    }

}
