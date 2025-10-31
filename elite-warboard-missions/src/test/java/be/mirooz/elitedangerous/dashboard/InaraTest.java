package be.mirooz.elitedangerous.dashboard;

import be.mirooz.elitedangerous.dashboard.service.EdToolsService;
import be.mirooz.elitedangerous.dashboard.service.InaraService;
import be.mirooz.elitedangerous.dashboard.service.RouteService;
import be.mirooz.elitedangerous.lib.edtools.model.MiningHotspot;
import be.mirooz.elitedangerous.lib.inara.model.InaraCommoditiesStats;
import be.mirooz.elitedangerous.lib.inara.model.CommodityMaxSell;
import be.mirooz.elitedangerous.lib.inara.client.InaraClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class InaraTest {

    private static final InaraService inaraService = InaraService.getInstance();
    private static final RouteService routeService = RouteService.getInstance();
    private static final EdToolsService edtoolsService=EdToolsService.getInstance();
    private static final InaraClient inaraClient = new InaraClient();

    @Test
    public void testFetchCommoditiesMaxSell() throws Exception {
        // Test de récupération de la liste des commodités depuis Inara
        List<CommodityMaxSell> commodities = inaraClient.fetchCommoditiesMaxSell();
        
        // Vérifications
        Assertions.assertNotNull(commodities, "La liste des commodités ne devrait pas être nulle");
        Assertions.assertFalse(commodities.isEmpty(), "La liste des commodités ne devrait pas être vide");
        
        // Afficher les 10 premières commodités
        System.out.println("\n=== Top 10 Commodities with Max Sell Prices ===");
        commodities.stream()
                .filter(c -> c.getMaxSellPrice() > 0)
                .sorted((a, b) -> Integer.compare(b.getMaxSellPrice(), a.getMaxSellPrice()))
                .limit(10)
                .forEach(c -> System.out.printf("%-40s | ID: %-6s | Max Sell: %,d Cr%n", 
                        c.getCommodityName(), 
                        c.getInaraId(),
                        c.getMaxSellPrice()));
        
        System.out.println("\nTotal commodities parsed: " + commodities.size());
    }

}
