package be.mirooz.elitedangerous.dashboard;

import be.mirooz.elitedangerous.biologic.BioSpecies;
import be.mirooz.elitedangerous.dashboard.handlers.events.journalevents.ScanHandler;
import be.mirooz.elitedangerous.dashboard.model.exploration.ACelesteBody;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.PlaneteRegistry;
import be.mirooz.elitedangerous.service.BioSpeciesService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

public class BioForgeTest {
    @Test
    public void testGetSpecies() throws Exception {
        List<BioSpecies> bioSpecies  = BioSpeciesService.getInstance().getSpecies();
        bioSpecies.stream().filter(e -> e.getName().equals("Tubus")).forEach(
                e -> System.out.println(e.getFullName())
        );
    }
    @Test
    public void scanPlanet() throws IOException, URISyntaxException {
        String jsonNode = """
                {
                   "timestamp" : "2025-11-04T23:03:00Z",
                   "event" : "Scan",
                   "ScanType" : "Detailed",
                   "BodyName" : "Swoilz EC-Y c16-1 A 1 a",
                   "BodyID" : 20,
                   "Parents" : [ {
                     "Planet" : 17
                   }, {
                     "Null" : 16
                   }, {
                     "Star" : 1
                   }, {
                     "Null" : 0
                   } ],
                   "StarSystem" : "Swoilz EC-Y c16-1",
                   "SystemAddress" : 360273548178,
                   "DistanceFromArrivalLS" : 2388.501901,
                   "TidalLock" : true,
                   "TerraformState" : "",
                   "PlanetClass" : "Rocky body",
                   "Atmosphere" : "thin ammonia atmosphere",
                   "AtmosphereType" : "Ammonia",
                   "AtmosphereComposition" : [ {
                     "Name" : "Ammonia",
                     "Percent" : 100.0
                   } ],
                   "Volcanism" : "",
                   "MassEM" : 0.006733,
                   "Radius" : 1320585.625,
                   "SurfaceGravity" : 1.538739,
                   "SurfaceTemperature" : 161.149033,
                   "SurfacePressure" : 153.413422,
                   "Landable" : true,
                   "Materials" : [ {
                     "Name" : "iron",
                     "Name_Localised" : "Fer",
                     "Percent" : 19.08436
                   }, {
                     "Name" : "sulphur",
                     "Name_Localised" : "Soufre",
                     "Percent" : 18.751884
                   }, {
                     "Name" : "carbon",
                     "Name_Localised" : "Carbone",
                     "Percent" : 15.768391
                   }, {
                     "Name" : "nickel",
                     "Percent" : 14.43461
                   }, {
                     "Name" : "phosphorus",
                     "Name_Localised" : "Phosphore",
                     "Percent" : 10.095194
                   }, {
                     "Name" : "manganese",
                     "Name_Localised" : "Manganèse",
                     "Percent" : 7.881647
                   }, {
                     "Name" : "germanium",
                     "Percent" : 5.51991
                   }, {
                     "Name" : "zinc",
                     "Percent" : 5.186415
                   }, {
                     "Name" : "niobium",
                     "Percent" : 1.304314
                   }, {
                     "Name" : "yttrium",
                     "Percent" : 1.139886
                   }, {
                     "Name" : "mercury",
                     "Name_Localised" : "Mercure",
                     "Percent" : 0.833383
                   } ],
                   "Composition" : {
                     "Ice" : 0.0,
                     "Rock" : 0.911083,
                     "Metal" : 0.088917
                   },
                   "SemiMajorAxis" : 3.874166488647461E9,
                   "Eccentricity" : 0.006662,
                   "OrbitalInclination" : 0.054466,
                   "Periapsis" : 142.389042,
                   "OrbitalPeriod" : 1454081.058502,
                   "AscendingNode" : 160.612219,
                   "MeanAnomaly" : 136.238151,
                   "RotationPeriod" : 1454085.276453,
                   "AxialTilt" : -0.3138,
                   "WasDiscovered" : true,
                   "WasMapped" : false,
                   "WasFootfalled" : false
                 }
                """;
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonNode);
        ScanHandler handler = new ScanHandler();
        handler.handle(root);
        System.out.println(PlaneteRegistry.getInstance().getAllPlanetes());
        Optional<ACelesteBody> OplaneteDetail = PlaneteRegistry.getInstance().getAllPlanetes().stream().findFirst();
    }
}
