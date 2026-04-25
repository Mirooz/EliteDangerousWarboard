package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.ardentapi.model.CommoditiesStats;
import be.mirooz.ardentapi.model.StationMarket;
import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.Mineral;
import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderShip.ShipCargo;
import be.mirooz.elitedangerous.dashboard.model.events.ProspectedAsteroid;
import be.mirooz.elitedangerous.dashboard.service.listeners.MiningEventNotificationService;
import be.mirooz.elitedangerous.dashboard.service.listeners.MiningSessionNotificationService;
import be.mirooz.elitedangerous.dashboard.service.webservice.ArdentApiService;
import be.mirooz.elitedangerous.lib.edtools.model.MiningHotspot;
import be.mirooz.ardentapi.model.CommodityMaxSell;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
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

    private static final int MAX_PROSPECTED_ASTEROIDS = 50;
    private final List<ProspectedAsteroid> prospectedAsteroids = new ArrayList<>();
    private final MiningEventNotificationService miningEventNotifications = MiningEventNotificationService.getInstance();
    private final MiningSessionNotificationService miningSessionNotificationService =
            MiningSessionNotificationService.getInstance();

    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();
    public final ArdentApiService ardentApiService = ArdentApiService.getInstance();
    private final EdToolsService edToolsService = EdToolsService.getInstance();

    private MiningService() {
        miningSessionNotificationService.addSessionEndListener(this::onMiningSessionEnd);
    }

    private void onMiningSessionEnd() {
        clearAllProspectors();
    }

    public static MiningService getInstance() {
        if (instance == null) {
            instance = new MiningService();
        }
        return instance;
    }

    /**
     * Enregistre un prospect d’astéroïde (ordre d’arrivée, buffer borné, non persisté disque).
     */
    public synchronized void registerProspectedAsteroid(ProspectedAsteroid asteroid) {
        if (asteroid == null) {
            return;
        }
        if (!prospectedAsteroids.isEmpty()
                && prospectedAsteroids.get(prospectedAsteroids.size() - 1).equals(asteroid)) {
            return;
        }
        prospectedAsteroids.add(asteroid);
        while (prospectedAsteroids.size() > MAX_PROSPECTED_ASTEROIDS) {
            prospectedAsteroids.remove(0);
        }
        miningEventNotifications.notifyProspectorAdded(asteroid);
    }

    /**
     * Récupère tous les prospecteurs
     */
    public List<ProspectedAsteroid> getAllProspectors() {
        synchronized (this) {
            return new ArrayList<>(prospectedAsteroids);
        }
    }

    /**
     * Nettoie tous les prospecteurs (utilisé lors de la fin de session de minage)
     */
    public void clearAllProspectors() {
        synchronized (this) {
            prospectedAsteroids.clear();
        }
        miningEventNotifications.notifyRegistryCleared();
        System.out.println("🗑️ Tous les prospecteurs ont été nettoyés");
    }

    /**
     * Récupère le dernier prospecteur
     */
    public Optional<ProspectedAsteroid> getLastProspector() {
        synchronized (this) {
            if (prospectedAsteroids.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(prospectedAsteroids.get(prospectedAsteroids.size() - 1));
        }
    }

    /**
     * Récupère le cargo actuel
     */
    public ShipCargo getCargo() {
        if (commanderStatus.getShip() == null) {
            return null;
        }
        if (commanderStatus.getShip().getJsonShipCargo() !=null){
            return commanderStatus.getShip().getJsonShipCargo();
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
    public CompletableFuture<List<CommoditiesStats>> findMineralStation(Mineral mineral, String sourceSystem, int maxDistance, int minDemand, boolean largePad, boolean includeFleetCarrier) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return ardentApiService.fetchMinerMarket(mineral, sourceSystem, maxDistance, minDemand, largePad, includeFleetCarrier);
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

    public ArdentApiService getArdentApiService() {
        return ardentApiService;
    }

    /**
     * Récupère le marché complet d'une station de manière asynchrone
     * 
     * @param stationUrl L'URL de la station
     * @return Un CompletableFuture contenant le marché de la station
     */
    public CompletableFuture<StationMarket> fetchStationMarket(String marketId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return ardentApiService.fetchStationMarket(marketId);
            } catch (Exception e) {
                throw new RuntimeException("Erreur lors de la récupération du marché de station", e);
            }
        });
    }

    /**
     * Récupère la capacité de cargo actuelle
     */
    public int getCurrentCargoCapacity() {
        if (commanderStatus.getShip() != null) {
            return commanderStatus.getShip().getMaxCapacity();
        }
        return 0; // Fallback
    }

    public CompletableFuture <List<CommodityMaxSell>> fetchCommoditiesMaxSell() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return ardentApiService.fetchCommoditiesMaxSell();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
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
     * Recherche les hotspots de minage pour une station spécifique
     */
    public CompletableFuture<MiningHotspot> findMiningHotspotsForStation(CommoditiesStats station, Mineral mineral) {
        return getEdToolsService().findMiningHotspots(station.getSystemName(), mineral)
                .thenApply(hotspots -> {
                    getArdentApiService().setHotspots(hotspots);
                    if (hotspots != null && !hotspots.isEmpty()) {
                        // Stocker tous les hotspots dans InaraService pour la navigation
                        // Retourner le hotspot actuel (ou le premier si aucun n'est sélectionné)
                        MiningHotspot currentHotspot = getArdentApiService().getCurrentHotspot();
                        return Objects.requireNonNullElseGet(currentHotspot, () -> hotspots.stream()
                                .min(Comparator.comparingDouble(MiningHotspot::getDistanceFromReference))
                                .orElse(hotspots.get(0)));
                    }
                    return null;
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
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');

        DecimalFormat df = new DecimalFormat("#,##0", symbols);
        return df.format(price);
    }


    public void setCoreSession(boolean isCore) {
        if ( MiningStatsService.getInstance().isMiningInProgress() ) {
            MiningStatsService.getInstance().getCurrentMiningSession().ifPresent(
                    session -> session.setCoreSession(isCore)
            );
        }
    }
}
