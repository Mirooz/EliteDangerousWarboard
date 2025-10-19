package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.Mineral;
import be.mirooz.elitedangerous.dashboard.model.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.model.commander.CommanderShip.ShipCargo;
import be.mirooz.elitedangerous.dashboard.model.events.ProspectedAsteroid;
import be.mirooz.elitedangerous.dashboard.model.registries.ProspectedAsteroidRegistry;
import be.mirooz.elitedangerous.lib.edtools.model.MiningHotspot;
import be.mirooz.elitedangerous.lib.inara.model.InaraCommoditiesStats;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static be.mirooz.elitedangerous.commons.lib.models.commodities.LimpetType.LIMPET;

/**
 * Service pour gérer la logique métier du mining
 * <p>
 * Cette classe contient toute la logique métier liée au mining :
 * - Calculs de crédits estimés
 * - Recherche de routes de minage complètes
 * - Validation et formatage des données
 * - Gestion des minéraux et du cargo
 * <p>
 * Refactorisé depuis MiningController pour séparer la logique métier de l'interface utilisateur.
 */
public class MiningService {

    private static MiningService instance;

    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();
    private final ProspectedAsteroidRegistry prospectedRegistry = ProspectedAsteroidRegistry.getInstance();
    private final InaraService inaraService = InaraService.getInstance();
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
    public CompletableFuture<Optional<InaraCommoditiesStats>> findMineralPrice(Mineral mineral, String sourceSystem, int maxDistance, int minDemand, boolean largePad, boolean includeFleetCarrier) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return inaraService.fetchMinerMarket(mineral, sourceSystem, maxDistance, minDemand, largePad, includeFleetCarrier);
            } catch (Exception e) {
                throw new RuntimeException(e);
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
     * Calcule les CR estimés basés sur les minéraux dans le cargo
     */
    public long calculateEstimatedCredits() {
        try {
            long totalCredits = 0;
            Map<Mineral, Integer> minerals = getMinerals();
            if (minerals != null && !minerals.isEmpty()) {
                for (Map.Entry<Mineral, Integer> entry : minerals.entrySet()) {
                    Mineral mineralName = entry.getKey();
                    Integer quantity = entry.getValue();

                    if (quantity != null && quantity > 0) {
                        totalCredits += (long) mineralName.getPrice() * quantity;
                    }
                }
            }
            return totalCredits;
        } catch (Exception e) {
            // En cas d'erreur, retourner 0
            return 0;
        }
    }

    /**
     * Recherche une route de minage complète pour un minéral spécifique
     */
    public CompletableFuture<MiningRouteResult> searchMiningRoute(Mineral mineral, String sourceSystem, int maxDistance, int minDemand, boolean largePad, boolean includeFleetCarrier) {
        return findMineralPrice(mineral, sourceSystem, maxDistance, minDemand, largePad, includeFleetCarrier)
                .thenCompose(priceOpt -> {
                    if (priceOpt.isPresent()) {
                        InaraCommoditiesStats bestMarket = priceOpt.get();
                        return getEdToolsService().findMiningHotspots(bestMarket.getSystemName(), mineral)
                                .thenApply(hotspots -> {
                                    MiningHotspot bestHotspot = null;
                                    if (hotspots != null && !hotspots.isEmpty()) {
                                        bestHotspot = hotspots.stream()
                                                .min(Comparator.comparingDouble(MiningHotspot::getDistanceFromReference))
                                                .orElse(hotspots.get(0));
                                    }
                                    return new MiningRouteResult(bestMarket, bestHotspot);
                                });
                    } else {
                        return CompletableFuture.completedFuture(new MiningRouteResult(null, null));
                    }
                });
    }

    /**
     * Récupère la distance maximale depuis un champ de saisie avec validation
     */
    public int getMaxDistanceFromField(String distanceText, int defaultDistance) {
        if (distanceText != null && !distanceText.isEmpty()) {
            try {
                int distance = Integer.parseInt(distanceText);
                return Math.max(1, Math.min(distance, 1000)); // Limiter entre 1 et 1000
            } catch (NumberFormatException e) {
                return defaultDistance; // Valeur par défaut
            }
        }
        return defaultDistance; // Valeur par défaut
    }

    /**
     * Valide et nettoie le texte de distance pour ne garder que les chiffres
     */
    public String validateDistanceText(String text) {
        if (text == null) return "";
        return text.replaceAll("[^\\d]", "");
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

    /**
     * Classe interne pour encapsuler le résultat d'une recherche de route de minage
     */
    public static class MiningRouteResult {
        private final InaraCommoditiesStats market;
        private final MiningHotspot hotspot;

        public MiningRouteResult(InaraCommoditiesStats market, MiningHotspot hotspot) {
            this.market = market;
            this.hotspot = hotspot;
        }

        public InaraCommoditiesStats getMarket() {
            return market;
        }

        public MiningHotspot getHotspot() {
            return hotspot;
        }

        public boolean hasMarket() {
            return market != null;
        }

        public boolean hasHotspot() {
            return hotspot != null;
        }
    }
}
