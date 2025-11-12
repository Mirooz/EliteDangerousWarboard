package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.biologic.*;
import be.mirooz.elitedangerous.dashboard.model.exploration.BioSpeciesMatcher;
import be.mirooz.elitedangerous.dashboard.model.exploration.PlaneteDetail;
import be.mirooz.elitedangerous.dashboard.model.registries.PlaneteRegistry;
import be.mirooz.elitedangerous.service.BioSpeciesService;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.net.URISyntaxException;
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
            String atmosphereTypeStr = jsonNode.path("AtmosphereType").asText();
            AtmosphereType atmosphereType = parseAtmosphereType(atmosphereStr, atmosphereTypeStr);

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
                    .starSystem(starSystem)
                    .systemAddress(systemAddress)
                    .bodyID(bodyID)
                    .planetClass(bodyType)
                    .temperature(surfaceTemperature > 0 ? surfaceTemperature : null)
                    .pressureAtm(pressureAtm > 0 ? pressureAtm : null)
                    .gravityG(gravityG > 0 ? gravityG : null)
                    .landable(landable)
                    .atmosphere(atmosphereType)
                    .volcanism(volcanismType)
                    .materials(materials.isEmpty() ? null : materials)
                    .wasMapped(wasMapped)
                    .wasFootfalled(wasFootfalled)
                    .wasDiscovered(wasDiscovered)
                    .build();

            // Log des informations principales
            System.out.printf("🔍 Scan: %s (%s) in %s%n", bodyName,
                    bodyType != null ? bodyType.getDisplayName() : planetClassStr, starSystem);
            if (atmosphereType != null) {
                System.out.printf("   Atmosphere: %s%n", atmosphereType.getDisplayName());
            } else if (!atmosphereStr.isEmpty()) {
                System.out.printf("   Atmosphere: %s (%s)%n", atmosphereStr, atmosphereTypeStr);
            }
            if (volcanismType != null) {
                System.out.printf("   Volcanism: %s%n", volcanismType.getDisplayName());
            } else if (!volcanismStr.isEmpty()) {
                System.out.printf("   Volcanism: %s%n", volcanismStr);
            }
            if (landable) {
                System.out.printf("   Landable: Yes (Gravity: %.2fG, Temp: %.1fK, Pressure: %.4f atm)%n",
                        gravityG, surfaceTemperature, pressureAtm);
            }

            // Enregistrement de la planète dans le registre
            PlaneteRegistry.getInstance().addOrUpdatePlanete(planeteDetail);

            // Vérification des espèces biologiques possibles sur cette planète
            if (landable) {
                try {
                    List<BioSpecies> allSpecies = BioSpeciesService.getInstance().getSpecies();
                    List<BioSpecies> matchingSpecies = allSpecies.stream()
                            .filter(species -> BioSpeciesMatcher.matches(planeteDetail, species))
                            .collect(Collectors.toList());

                    if (!matchingSpecies.isEmpty()) {
                        System.out.printf("   🌱 Espèces biologiques possibles (%d):%n", matchingSpecies.size());
                        matchingSpecies.forEach(species ->
                                {
                                    if (species.getVariantMethod().equals(VariantMethods.SURFACE_MATERIALS) || species.getColorConditionName().equals("M")) {
                                        System.out.printf("      - %s - %d%n", species.getFullName(), species.getBaseValue());
                                    }
                                }
                        );
                    }
                } catch (URISyntaxException | IOException e) {
                    System.err.println("❌ Erreur lors du chargement des espèces biologiques: " + e.getMessage());
                }
            }

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
    private AtmosphereType parseAtmosphereType(String atmosphere, String atmosphereType) {
        if (atmosphere == null || atmosphere.isEmpty()) {
            return AtmosphereType.NO_ATMOSPHERE;
        }

        if (atmosphereType == null || atmosphereType.isEmpty()) {
            // Si pas d'AtmosphereType mais qu'il y a une atmosphère, essayer de parser directement
            return AtmosphereType.fromString(atmosphere);
        }

        String normalized = atmosphere.toLowerCase().trim();
        String type = atmosphereType.trim();

        // Déterminer la densité (thin/thick/hot)
        String density = "";
        if (normalized.contains("thin")) {
            density = "Thin ";
        } else if (normalized.contains("thick")) {
            density = "Thick ";
        } else if (normalized.contains("hot")) {
            density = "Hot ";
        }

        // Vérifier si c'est "rich"
        boolean isRich = normalized.contains("rich");

        // Construire la chaîne complète
        String fullAtmosphereType;
        if (isRich) {
            fullAtmosphereType = density + type + "-rich";
        } else {
            fullAtmosphereType = density + type;
        }

        // Essayer de matcher avec l'enum
        AtmosphereType result = AtmosphereType.fromString(fullAtmosphereType);

        // Si pas de match, essayer sans la densité
        if (result == null) {
            if (isRich) {
                result = AtmosphereType.fromString(type + "-rich");
            } else {
                result = AtmosphereType.fromString(type);
            }
        }

        return result;
    }
}

