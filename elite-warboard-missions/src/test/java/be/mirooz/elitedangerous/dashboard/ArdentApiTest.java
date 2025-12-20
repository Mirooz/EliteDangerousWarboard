package be.mirooz.elitedangerous.dashboard;

import be.mirooz.ardentapi.client.ArdentApiClient;
import be.mirooz.ardentapi.model.CommodityMaxSell;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class ArdentApiTest {

    private static final ArdentApiClient ardentApiClient = new ArdentApiClient();

    @Test
    public void testFetchCommoditiesMaxSell() throws Exception {
        // Test de récupération de la liste des commodités depuis Inara
        List<CommodityMaxSell> commodities = ardentApiClient.fetchCommoditiesMaxSell();
        
        // Vérifications
        Assertions.assertNotNull(commodities, "La liste des commodités ne devrait pas être nulle");
        Assertions.assertFalse(commodities.isEmpty(), "La liste des commodités ne devrait pas être vide");
        
        // Afficher les 10 premières commodités
        System.out.println("\n=== Top 10 Commodities with Max Sell Prices ===");
        commodities.stream()
                .filter(c -> c.getMaxSellPrice() > 0)
                .sorted((a, b) -> Integer.compare(b.getMaxSellPrice(), a.getMaxSellPrice()))
                .limit(10)
                .forEach(c -> System.out.printf("%-40s |  Max Sell: %,d Cr%n",
                        c.getCommodityName(),
                        c.getMaxSellPrice()));
        
        System.out.println("\nTotal commodities parsed: " + commodities.size());
    }

}
