package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.biologic.*;
import be.mirooz.elitedangerous.dashboard.model.exploration.BioSpeciesMatcher;
import be.mirooz.elitedangerous.dashboard.model.exploration.PlaneteDetail;
import be.mirooz.elitedangerous.dashboard.model.registries.PlaneteRegistry;
import be.mirooz.elitedangerous.service.BioSpeciesService;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handler pour l'événement Scan du journal Elite Dangerous
 * <p>
 * Traite les scans détaillés des corps célestes (planètes, lunes, etc.)
 * et extrait les informations sur l'atmosphère, le volcanisme, les matériaux, etc.
 */
public class ScanHandler implements JournalEventHandler {

    @Override
    public String getEventType() {
        return "Scan";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            // Informations de base
            String timestamp = jsonNode.path("timestamp").asText();
            String scanType = jsonNode.path("ScanType").asText();
            String bodyName = jsonNode.path("BodyName").asText();
            int bodyID = jsonNode.path("BodyID").asInt();
            String starSystem = jsonNode.path("StarSystem").asText();
            long systemAddress = jsonNode.path("SystemAddress").asLong();

            // Informations sur le corps - conversion en enums
            String planetClassStr = jsonNode.path("PlanetClass").asText();
            BodyType bodyType = BodyType.fromString(planetClassStr);

            String atmosphereStr = jsonNode.path("Atmosphere").asText();
            AtmosphereType atmosphereType = parseAtmosphereType(atmosphereStr);

            String volcanismStr = jsonNode.path("Volcanism").asText();
            VolcanismType volcanismType = VolcanismType.fromString(volcanismStr);

            // Propriétés physiques
            double massEM = jsonNode.path("MassEM").asDouble(0.0);
            double radius = jsonNode.path("Radius").asDouble(0.0);
            double surfaceGravity = jsonNode.path("SurfaceGravity").asDouble(0.0);
            double surfaceTemperature = jsonNode.path("SurfaceTemperature").asDouble(0.0);
            double surfacePressure = jsonNode.path("SurfacePressure").asDouble(0.0);
            boolean landable = jsonNode.path("Landable").asBoolean(false);

            // Distance depuis l'arrivée
            double distanceFromArrivalLS = jsonNode.path("DistanceFromArrivalLS").asDouble(0.0);

            // Tidal lock et terraform state
            boolean tidalLock = jsonNode.path("TidalLock").asBoolean(false);
            String terraformState = jsonNode.path("TerraformState").asText();

            // Composition de l'atmosphère
            if (jsonNode.has("AtmosphereComposition") && jsonNode.get("AtmosphereComposition").isArray()) {
                jsonNode.get("AtmosphereComposition").forEach(comp -> {
                    String name = comp.path("Name").asText();
                    double percent = comp.path("Percent").asDouble(0.0);
                    // Traiter la composition de l'atmosphère si nécessaire
                });
            }

            // Matériaux de surface
            if (jsonNode.has("Materials") && jsonNode.get("Materials").isArray()) {
                jsonNode.get("Materials").forEach(material -> {
                    String name = material.path("Name").asText();
                    double percent = material.path("Percent").asDouble(0.0);
                    // Traiter les matériaux si nécessaire
                });
            }

            // Composition (Ice, Rock, Metal)
            if (jsonNode.has("Composition")) {
                JsonNode composition = jsonNode.get("Composition");
                double ice = composition.path("Ice").asDouble(0.0);
                double rock = composition.path("Rock").asDouble(0.0);
                double metal = composition.path("Metal").asDouble(0.0);
                // Traiter la composition si nécessaire
            }

            // Paramètres orbitaux
            if (jsonNode.has("SemiMajorAxis")) {
                double semiMajorAxis = jsonNode.path("SemiMajorAxis").asDouble(0.0);
                double eccentricity = jsonNode.path("Eccentricity").asDouble(0.0);
                double orbitalInclination = jsonNode.path("OrbitalInclination").asDouble(0.0);
                double periapsis = jsonNode.path("Periapsis").asDouble(0.0);
                double orbitalPeriod = jsonNode.path("OrbitalPeriod").asDouble(0.0);
                // Traiter les paramètres orbitaux si nécessaire
            }

            // Statut de découverte
            boolean wasDiscovered = jsonNode.path("WasDiscovered").asBoolean(false);
            boolean wasMapped = jsonNode.path("WasMapped").asBoolean(false);
            boolean wasFootfalled = jsonNode.path("WasFootfalled").asBoolean(false);

            // Extraction des matériaux
            Map<String, Double> materials = new HashMap<>();
            if (jsonNode.has("Materials") && jsonNode.get("Materials").isArray()) {
                jsonNode.get("Materials").forEach(material -> {
                    String name = material.path("Name").asText();
                    double percent = material.path("Percent").asDouble(0.0);
                    if (!name.isEmpty()) {
                        materials.put(name, percent);
                    }
                });
            }

            // Conversion des unités
            double pressureAtm = PlaneteDetail.pascalToAtm(surfacePressure);
            double gravityG = PlaneteDetail.ms2ToG(surfaceGravity);

            // Création de l'objet PlaneteDetail
            PlaneteDetail planeteDetail = PlaneteDetail.builder()
                    .bodyName(bodyName)
                    .timestamp(timestamp)
                    .starSystem(starSystem)
                    .systemAddress(systemAddress)
                    .bodyID(bodyID)
                    .planetClass(bodyType)
                    .temperature(surfaceTemperature >= 0 ? surfaceTemperature : null)
                    .pressureAtm(pressureAtm >= 0 ? pressureAtm : null)
                    .gravityG(gravityG >= 0 ? gravityG : null)
                    .landable(landable)
                    .atmosphere(atmosphereType)
                    .volcanism(volcanismType)
                    .materials(materials.isEmpty() ? null : materials)
                    .wasMapped(wasMapped)
                    .wasFootfalled(wasFootfalled)
                    .wasDiscovered(wasDiscovered)
                    .build();

            // Log des informations principales
         /*   System.out.printf("🔍 Scan: %s (%s) in %s%n", bodyName,
                    bodyType != null ? bodyType.getDisplayName() : planetClassStr, starSystem);
            if (atmosphereType != null) {
                System.out.printf("   Atmosphere: %s%n", atmosphereType.getDisplayName());
            } else if (!atmosphereStr.isEmpty()) {
                System.out.printf("   Atmosphere: null %n");
            }
            if (volcanismType != null) {
                System.out.printf("   Volcanism: %s%n", volcanismType.getDisplayName());
            } else if (!volcanismStr.isEmpty()) {
                System.out.printf("   Volcanism: %s%n", volcanismStr);
            }
            if (landable) {
                System.out.printf("   Landable: Yes (Gravity: %.2fG, Temp: %.1fK, Pressure: %.4f atm)%n",
                        gravityG, surfaceTemperature, pressureAtm);
            }*/

            // Enregistrement de la planète dans le registre
            PlaneteRegistry.getInstance().addOrUpdatePlanete(planeteDetail);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du traitement de l'événement Scan: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Parse l'atmosphère depuis les champs "Atmosphere" et "AtmosphereType" du journal.
     * Le format dans le journal est "thin ammonia atmosphere" avec AtmosphereType="Ammonia".
     * On doit construire "Thin Ammonia" pour matcher l'enum.
     */
    private AtmosphereType parseAtmosphereType(String atmosphere) {
        if (atmosphere == null || atmosphere.isEmpty()) {
            return AtmosphereType.NO_ATMOSPHERE;
        }

        if (atmosphere.contains(" atmosphere")) {
            atmosphere = atmosphere.replace(" atmosphere", "");
        }
        return AtmosphereType.fromString(atmosphere);

    }
}

