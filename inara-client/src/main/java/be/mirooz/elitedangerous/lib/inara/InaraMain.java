package be.mirooz.elitedangerous.lib.inara;

import be.mirooz.elitedangerous.lib.inara.service.InaraService;

import java.io.IOException;


public class InaraMain {
    public static void main(String[] args) throws IOException {

        InaraService service = new InaraService();
        try {
            System.out.println("🚀 Démarrage de la recherche de tous les marchés de minéraux...");
            var commodities = service.fetchAllMinerMarkets("belanit", 100, 500, false);
            System.out.println("📊 Résultats trouvés: " + commodities.size() + " commodités");
            
            // Afficher les 10 premiers résultats
            commodities.stream()
                .limit(10)
                .forEach(commodity -> 
                    System.out.println(commodity.getName() + " - " + 
                                     commodity.getStationName() + " - " + 
                                     commodity.getPrice() + " Cr")
                );
        } finally {
            service.shutdown();
        }
    }
}
