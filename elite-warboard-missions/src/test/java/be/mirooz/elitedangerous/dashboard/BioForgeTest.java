package be.mirooz.elitedangerous.dashboard;

import be.mirooz.elitedangerous.biologic.BioSpecies;
import be.mirooz.elitedangerous.dashboard.handlers.events.journalevents.ScanHandler;
import be.mirooz.elitedangerous.dashboard.model.exploration.PlaneteDetail;
import be.mirooz.elitedangerous.dashboard.model.registries.PlaneteRegistry;
import be.mirooz.elitedangerous.service.BioSpeciesService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
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
  "timestamp" : "2025-11-04T21:53:20Z",
  "event" : "Scan",
  "ScanType" : "Detailed",
  "BodyName" : "Swoilz UL-S b22-2 7",
  "BodyID" : 10,
  "Parents" : [ {
    "Star" : 0
  } ],
  "StarSystem" : "Swoilz UL-S b22-2",
  "SystemAddress" : 5079468746433,
  "DistanceFromArrivalLS" : 5228.281524,
  "TidalLock" : false,
  "TerraformState" : "",
  "PlanetClass" : "Icy body",
  "Atmosphere" : "thin neon atmosphere",
  "AtmosphereType" : "Neon",
  "AtmosphereComposition" : [ {
    "Name" : "Neon",
    "Percent" : 100.0
  } ],
  "Volcanism" : "",
  "MassEM" : 0.295608,
  "Radius" : 5347970.5,
  "SurfaceGravity" : 4.11953,
  "SurfaceTemperature" : 27.384855,
  "SurfacePressure" : 201.871338,
  "Landable" : true,
  "Materials" : [ {
    "Name" : "sulphur",
    "Name_Localised" : "Soufre",
    "Percent" : 22.395512
  }, {
    "Name" : "carbon",
    "Name_Localised" : "Carbone",
    "Percent" : 18.832306
  }, {
    "Name" : "iron",
    "Name_Localised" : "Fer",
    "Percent" : 15.543619
  }, {
    "Name" : "phosphorus",
    "Name_Localised" : "Phosphore",
    "Percent" : 12.056766
  }, {
    "Name" : "nickel",
    "Percent" : 11.756542
  }, {
    "Name" : "chromium",
    "Name_Localised" : "Chrome",
    "Percent" : 6.990481
  }, {
    "Name" : "manganese",
    "Name_Localised" : "Manganèse",
    "Percent" : 6.419356
  }, {
    "Name" : "germanium",
    "Percent" : 3.242417
  }, {
    "Name" : "niobium",
    "Percent" : 1.062323
  }, {
    "Name" : "molybdenum",
    "Name_Localised" : "Molybdène",
    "Percent" : 1.014989
  }, {
    "Name" : "antimony",
    "Name_Localised" : "Antimoine",
    "Percent" : 0.685688
  } ],
  "Composition" : {
    "Ice" : 0.683027,
    "Rock" : 0.212245,
    "Metal" : 0.104728
  },
  "SemiMajorAxis" : 1.5657658576965332E12,
  "Eccentricity" : 0.002992,
  "OrbitalInclination" : 1.557112,
  "Periapsis" : 232.975796,
  "OrbitalPeriod" : 1.763253033161163E9,
  "AscendingNode" : 10.035679,
  "MeanAnomaly" : 249.750288,
  "RotationPeriod" : 93730.787254,
  "AxialTilt" : 0.680795,
  "WasDiscovered" : false,
  "WasMapped" : false,
  "WasFootfalled" : false
}""";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonNode);
        ScanHandler handler = new ScanHandler();
        handler.handle(root);
        System.out.println(PlaneteRegistry.getInstance().getAllPlanetes());
        Optional<PlaneteDetail> OplaneteDetail = PlaneteRegistry.getInstance().getAllPlanetes().stream().findFirst();


    }
}
