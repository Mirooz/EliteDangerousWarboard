package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.Mineral;
import be.mirooz.elitedangerous.dashboard.model.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.model.commander.CommanderShip.ShipCargo;
import be.mirooz.elitedangerous.dashboard.model.events.ProspectedAsteroid;
import be.mirooz.elitedangerous.dashboard.model.registries.ProspectedAsteroidRegistry;
import be.mirooz.elitedangerous.lib.inara.client.InaraClient;
import be.mirooz.elitedangerous.lib.inara.model.InaraCommoditiesStats;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static be.mirooz.elitedangerous.commons.lib.models.commodities.LimpetType.LIMPET;

/**
 * Service pour gérer la logique métier du mining
 */
public class MiningService {

    private static MiningService instance;

    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();
    private final ProspectedAsteroidRegistry prospectedRegistry = ProspectedAsteroidRegistry.getInstance();
    private final InaraClient inaraClient = new InaraClient();
    private final EdToolsService edToolsService = EdToolsService.getInstance();

    private MiningService() {
    }

    public static MiningService getInstance() {
        if (instance == null) {
            instance = new MiningService();
        }
        return instance;
    }

    /**
     * Récupère tous les prospecteurs
     */
    public Deque<ProspectedAsteroid> getAllProspectors() {
        return prospectedRegistry.getAll();
    }

    /**
     * Récupère le dernier prospecteur
     */
    public Optional<ProspectedAsteroid> getLastProspector() {
        Deque<ProspectedAsteroid> prospectors = getAllProspectors();
        return prospectors.isEmpty() ? Optional.empty() : Optional.of(prospectors.peekLast());
    }

    /**
     * Récupère le cargo actuel
     */
    public ShipCargo getCargo() {
        if (commanderStatus.getShip() == null) {
            return null;
        }
        return commanderStatus.getShip().getShipCargo();
    }

    /**
     * Récupère le nombre de limpets dans le cargo
     */
    public int getLimpetsCount() {
        ShipCargo cargo = getCargo();
        if (cargo == null) {
            return 0;
        }
        return cargo.getCommodities().getOrDefault(LIMPET, 0);
    }

    /**
     * Récupère les minéraux du cargo (exclut les limpets)
     */
    public Map<Mineral, Integer> getMinerals() {
        ShipCargo cargo = getCargo();
        if (cargo == null) {
            return Collections.emptyMap();
        }

        return cargo.getCommodities().entrySet().stream()
                .filter(entry -> entry.getKey() instanceof Mineral)
                .collect(Collectors.toMap(
                        entry -> (Mineral) entry.getKey(),
                        Map.Entry::getValue
                ));
    }

    /**
     * Recherche le prix d'un minéral
     */

    /**
     * Recherche le prix d'un minéral avec option Fleet Carrier
     */
    public CompletableFuture<Optional<InaraCommoditiesStats>> findMineralPrice(Mineral mineral, String sourceSystem, int maxDistance, int minDemand, boolean largePad,boolean includeFleetCarrier) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<InaraCommoditiesStats> commodities = inaraClient.fetchMinerMarket(
                        mineral, sourceSystem, maxDistance, minDemand, largePad,includeFleetCarrier);

                if (commodities == null || commodities.isEmpty()) {
                    return Optional.empty();
                }
                Optional<InaraCommoditiesStats> bestOpt = commodities.stream()
                        .filter(c -> c.getSystemDistance() <= maxDistance)
                        .filter(c -> c.isFleetCarrier() ? minDemand<= c.getDemand() : minDemand*4 <= c.getDemand())
                        .max(Comparator.comparingDouble(InaraCommoditiesStats::getPrice));

                bestOpt.ifPresent(best -> mineral.setPrice(best.getPrice()));

                return bestOpt;

            } catch (Exception e) {
                return Optional.empty();
            }
        });
    }


    /**
     * Récupère le système actuel du commandant
     */
    public String getCurrentSystem() {
        if (commanderStatus.getCurrentStarSystem() != null) {
            return commanderStatus.getCurrentStarSystem();
        }
        return "Sol"; // Fallback
    }

    /**
     * Récupère le service EdTools
     */
    public EdToolsService getEdToolsService() {
        return edToolsService;
    }

    /**
     * Récupère la capacité de cargo actuelle
     */
    public int getCurrentCargoCapacity() {
        if (commanderStatus.getShip() != null && commanderStatus.getShip().getShipCargo() != null) {
            return commanderStatus.getShip().getShipCargo().getMaxCapacity();
        }
        return 0; // Fallback
    }

    /**
     * Formate un prix avec des séparateurs de milliers
     */
    public String formatPrice(int price) {
        return formatPrice((long) price);
    }
    public String formatPrice(long price) {
        return String.format("%,d", price).replace(",", ".");
    }
}
