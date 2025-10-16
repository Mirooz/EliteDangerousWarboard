package be.mirooz.elitedangerous.dashboard;

import be.mirooz.elitedangerous.dashboard.service.EdToolsService;
import be.mirooz.elitedangerous.dashboard.service.InaraService;
import be.mirooz.elitedangerous.dashboard.service.RouteService;
import be.mirooz.elitedangerous.lib.edtools.model.MiningHotspot;
import be.mirooz.elitedangerous.lib.inara.model.InaraCommoditiesStats;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class InaraTest {

    private static final InaraService inaraService = InaraService.getInstance();
    private static final RouteService routeService = RouteService.getInstance();
    private static final EdToolsService edtoolsService=EdToolsService.getInstance();

    @Test
    void testFetchAllMinerMarketsAroundSol() throws Exception {
        // Arrange
        String sourceSystem = "Sol";
        int distance= 100;
        int demand = 5000;
        boolean largePad = false;

        // Act
        CompletableFuture<List<InaraCommoditiesStats>> future = inaraService.fetchAllMinerMarkets(sourceSystem,distance,demand,largePad);

        // Attendre max 30 secondes pour la complétion
        List<InaraCommoditiesStats> commodities = future.get(30, TimeUnit.SECONDS);

        // Assert
        Assertions.assertNotNull(commodities, "La liste ne doit pas être nulle");
        Assertions.assertFalse(commodities.isEmpty(), "La liste de commodities ne doit pas être vide");

        // Log de debug
        System.out.println("✅ " + commodities.size() + " commodities trouvées autour de " + sourceSystem);
        commodities
                .forEach(c -> System.out.println(c.getCoreMineral().getInaraName() + " - " + c.getPrice() + " Cr @ " + c.getStationName() + " @ " + c.getSystemDistance() + " supply : " + c.getSupply()));

        System.out.println("Best InaraCommoditiesStats : ");
        InaraCommoditiesStats bestMinerInaraCommoditiesStats = routeService.findBestMinerCommodity(commodities,demand);
        System.out.println(bestMinerInaraCommoditiesStats);

        CompletableFuture<List<MiningHotspot>> futureEdtool = edtoolsService.findMiningHotspots(bestMinerInaraCommoditiesStats.getSystemName(), bestMinerInaraCommoditiesStats.getCoreMineral());

        // Attendre max 30 secondes pour la complétion
        List<MiningHotspot> miningHotspots = futureEdtool.get(30, TimeUnit.SECONDS);
        MiningHotspot bestHotspot = routeService.findBestHotspot(miningHotspots);
        System.out.println(bestHotspot);

        System.out.println("Mining route : ");
        System.out.println(bestMinerInaraCommoditiesStats.getCoreMineral().getInaraName()
                +"("+ bestMinerInaraCommoditiesStats.getPrice()+")"
                + " : " +bestHotspot.getSystemName()
                + " |" + bestHotspot.getRingName() + " -- " + bestHotspot.getDistanceFromReference() + " AL " + "-> " + bestMinerInaraCommoditiesStats.getSystemName()
                + " | " + bestMinerInaraCommoditiesStats.getStationName()
                + " (Demand : " + bestMinerInaraCommoditiesStats.getDemand() + ")");
    }
}
