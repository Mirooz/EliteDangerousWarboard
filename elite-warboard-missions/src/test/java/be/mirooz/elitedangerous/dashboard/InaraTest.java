package be.mirooz.elitedangerous.dashboard;

import be.mirooz.elitedangerous.dashboard.service.InaraService;
import be.mirooz.elitedangerous.dashboard.service.RouteService;
import be.mirooz.elitedangerous.lib.inara.model.Commodity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class InaraTest {

    private static final InaraService inaraService = InaraService.getInstance();
    private static final RouteService routeService = RouteService.getInstance();

    @Test
    void testFetchAllMinerMarketsAroundSol() throws Exception {
        // Arrange
        String sourceSystem = "Sol";

        // Act
        CompletableFuture<List<Commodity>> future = inaraService.fetchAllMinerMarkets(sourceSystem);

        // Attendre max 30 secondes pour la complétion
        List<Commodity> commodities = future.get(30, TimeUnit.SECONDS);

        // Assert
        Assertions.assertNotNull(commodities, "La liste ne doit pas être nulle");
        Assertions.assertFalse(commodities.isEmpty(), "La liste de commodities ne doit pas être vide");

        // Log de debug
        System.out.println("✅ " + commodities.size() + " commodities trouvées autour de " + sourceSystem);
        commodities
                .forEach(c -> System.out.println(c.getCoreMineral().getInaraName() + " - " + c.getPrice() + " Cr @ " + c.getStationName() + " @ " + c.getSystemDistance()));

        System.out.println("Best Commodity : ");
        System.out.println(routeService.findBestMinerCommodity(commodities));
    }
}
