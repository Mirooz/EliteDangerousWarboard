package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.backend.spansh.ExplorationMode;
import be.mirooz.elitedangerous.dashboard.model.navigation.NavRoute;
import be.mirooz.elitedangerous.dashboard.model.navigation.RouteSystem;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.ExplorationModeRegistry;
import be.mirooz.elitedangerous.dashboard.service.listeners.NavRouteNotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lecture de {@code NavRoute.json} du journal du jeu et état runtime observable des routes
 * par mode d'exploration (non persisté dans les JSON du dashboard).
 */
public class NavRouteService {

    private static final String NAV_ROUTE_FILE = "NavRoute.json";
    private static final NavRouteService INSTANCE = new NavRouteService();

    private final PreferencesService preferencesService = PreferencesService.getInstance();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<ExplorationMode, ObjectProperty<NavRoute>> routeMap = new HashMap<>();
    private final ObjectProperty<NavRoute> currentRoute = new SimpleObjectProperty<>(null);
    private final IntegerProperty remainingJumpsInRoute = new SimpleIntegerProperty(-1);

    private NavRouteService() {
        for (ExplorationMode mode : ExplorationMode.values()) {
            ObjectProperty<NavRoute> routeProperty = new SimpleObjectProperty<>(null);
            routeProperty.addListener((obs, oldRoute, newRoute) -> updateCurrentRoute());
            routeMap.put(mode, routeProperty);
        }
    }

    public static NavRouteService getInstance() {
        return INSTANCE;
    }

    /**
     * Lit le fichier NavRoute.json et met à jour la route pour le mode Free Exploration.
     */
    public void loadAndStoreNavRoute() {
        NavRoute navRoute = readNavRouteFile();

        if (navRoute != null) {
            setRouteForMode(navRoute, ExplorationMode.FREE_EXPLORATION);

            System.out.println("✅ Route de navigation Free Exploration chargée : " + navRoute.getRoute().size() + " systèmes");

            for (int i = 0; i < navRoute.getRoute().size(); i++) {
                RouteSystem system = navRoute.getRoute().get(i);
                if (i == 0) {
                    System.out.println("  📍 Système actuel : " + system.getSystemName()
                            + " (" + system.getStarClass() + ")");
                } else {
                    System.out.println("  → " + system.getSystemName()
                            + " (" + system.getStarClass() + ") - "
                            + String.format("%.2f", system.getDistanceFromPrevious()) + " AL");
                }
            }
        } else {
            clearRouteForMode(ExplorationMode.FREE_EXPLORATION);
        }
        NavRouteNotificationService.getInstance().notifyRouteRefreshRequired();
    }

    public void setCurrentRoute(NavRoute route) {
        try {
            ExplorationMode currentMode = ExplorationModeRegistry.getInstance().getCurrentMode();
            setRouteForMode(route, currentMode);
        } catch (Exception e) {
            setRouteForMode(route, ExplorationMode.FREE_EXPLORATION);
        }
    }

    public void setRouteForMode(NavRoute route, ExplorationMode mode) {
        ObjectProperty<NavRoute> routeProperty = routeMap.get(mode);
        if (routeProperty != null) {
            routeProperty.set(route);
        }
    }

    public NavRoute getRouteForMode(ExplorationMode mode) {
        ObjectProperty<NavRoute> routeProperty = routeMap.get(mode);
        return routeProperty != null ? routeProperty.get() : null;
    }

    private void updateCurrentRoute() {
        try {
            ExplorationMode currentMode = ExplorationModeRegistry.getInstance().getCurrentMode();
            NavRoute route = getRouteForMode(currentMode);
            currentRoute.set(route);
        } catch (Exception e) {
            ObjectProperty<NavRoute> freeExplorationProperty = routeMap.get(ExplorationMode.FREE_EXPLORATION);
            currentRoute.set(freeExplorationProperty != null ? freeExplorationProperty.get() : null);
        }
    }

    public ObjectProperty<NavRoute> getCurrentRouteProperty() {
        updateCurrentRoute();
        return currentRoute;
    }

    public NavRoute getCurrentRoute() {
        try {
            ExplorationMode currentMode = ExplorationModeRegistry.getInstance().getCurrentMode();
            return getRouteForMode(currentMode);
        } catch (Exception e) {
            return getRouteForMode(ExplorationMode.FREE_EXPLORATION);
        }
    }

    public boolean hasRoute() {
        NavRoute route = getCurrentRoute();
        return route != null && route.getRoute() != null && !route.getRoute().isEmpty();
    }

    public void setRemainingJumpsInRoute(int remainingJumps) {
        remainingJumpsInRoute.set(remainingJumps);
    }

    public IntegerProperty getRemainingJumpsInRouteProperty() {
        return remainingJumpsInRoute;
    }

    public int getRemainingJumpsInRoute() {
        return remainingJumpsInRoute.get();
    }

    public boolean hasTarget() {
        return remainingJumpsInRoute.get() >= 0;
    }

    public void clearTarget() {
        remainingJumpsInRoute.set(-1);
    }

    public void clearRouteForMode(ExplorationMode mode) {
        setRouteForMode(null, mode);
    }

    public void clearRoute() {
        try {
            ExplorationMode currentMode = ExplorationModeRegistry.getInstance().getCurrentMode();
            clearRouteForMode(currentMode);
        } catch (Exception e) {
            clearRouteForMode(ExplorationMode.FREE_EXPLORATION);
        }
    }

    /**
     * Lit le fichier NavRoute.json et crée la structure de données
     * Peut être appelée depuis le handler ou après le batch
     */
    public NavRoute readNavRouteFile() {
        try {
            String journalFolder = preferencesService.getJournalFolder();
            if (journalFolder == null || journalFolder.isEmpty()) {
                System.out.println("⚠️ Dossier journal non configuré");
                return null;
            }

            Path navRouteFilePath = Paths.get(journalFolder, NAV_ROUTE_FILE);
            if (!Files.exists(navRouteFilePath)) {
                System.out.println("⚠️ Fichier NavRoute.json non trouvé: " + navRouteFilePath);
                return null;
            }

            String navRouteContent = Files.readString(navRouteFilePath);
            if (navRouteContent == null || navRouteContent.trim().isEmpty()) {
                System.out.println("⚠️ Fichier NavRoute.json vide");
                return null;
            }

            JsonNode navRouteNode = objectMapper.readTree(navRouteContent);
            return parseNavRouteFromJson(navRouteNode);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la lecture du fichier NavRoute.json: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private NavRoute parseNavRouteFromJson(JsonNode jsonNode) {
        NavRoute navRoute = new NavRoute();

        if (jsonNode.has("timestamp")) {
            navRoute.setTimestamp(jsonNode.get("timestamp").asText());
        }

        if (!jsonNode.has("Route") || !jsonNode.get("Route").isArray()) {
            System.out.println("⚠️ Pas de tableau Route dans NavRoute.json");
            return null;
        }

        JsonNode routeArray = jsonNode.get("Route");
        List<RouteSystem> routeSystems = new ArrayList<>();

        double[] previousPosition = null;

        for (int i = 0; i < routeArray.size(); i++) {
            JsonNode systemNode = routeArray.get(i);

            String systemName = systemNode.has("StarSystem")
                    ? systemNode.get("StarSystem").asText() : "";
            long systemAddress = systemNode.has("SystemAddress")
                    ? systemNode.get("SystemAddress").asLong() : 0;
            String starClass = systemNode.has("StarClass")
                    ? systemNode.get("StarClass").asText() : "";

            double[] starPos = null;
            if (systemNode.has("StarPos") && systemNode.get("StarPos").isArray()) {
                JsonNode posArray = systemNode.get("StarPos");
                starPos = new double[3];
                starPos[0] = posArray.get(0).asDouble();
                starPos[1] = posArray.get(1).asDouble();
                starPos[2] = posArray.get(2).asDouble();
            }

            double distance = 0.0;
            if (i > 0 && previousPosition != null && starPos != null) {
                distance = calculateDistance(previousPosition, starPos);
            }

            RouteSystem routeSystem = new RouteSystem(
                    systemName,
                    systemAddress,
                    starClass,
                    starPos,
                    distance
            );

            routeSystems.add(routeSystem);
            previousPosition = starPos;
        }

        navRoute.setRoute(routeSystems);
        return navRoute;
    }

    private double calculateDistance(double[] pos1, double[] pos2) {
        if (pos1 == null || pos2 == null || pos1.length != 3 || pos2.length != 3) {
            return 0.0;
        }

        double dx = pos2[0] - pos1[0];
        double dy = pos2[1] - pos1[1];
        double dz = pos2[2] - pos1[2];

        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Récupère le nom du dernier système de la route dans NavRoute.json
     *
     * @return Le nom du dernier système, ou null si le fichier n'existe pas ou est vide
     */
    public String getFinalTargetSystem() {
        NavRoute navRoute = readNavRouteFile();
        if (navRoute != null && navRoute.getRoute() != null && !navRoute.getRoute().isEmpty()) {
            List<RouteSystem> route = navRoute.getRoute();
            RouteSystem lastSystem = route.get(route.size() - 1);
            return lastSystem.getSystemName();
        }
        return null;
    }
}
