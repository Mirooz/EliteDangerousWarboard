package be.mirooz.elitedangerous.lib.edtools;

import be.mirooz.elitedangerous.lib.edtools.client.EdToolsClient;
import be.mirooz.elitedangerous.lib.edtools.model.EdtoolResponse;
import be.mirooz.elitedangerous.lib.edtools.model.MiningHotspot;

import java.io.IOException;
import java.util.List;

public class EdtoolsMain {
    public static void main(String[] args) throws IOException {

        searchMiningHotspot();
    }

    private static void searchMiningHotspot() throws IOException {
        EdToolsClient client = new EdToolsClient();

        System.out.println("🚀 Démarrage de la recherche de hotspots Monazite...");
        List<MiningHotspot> hotspots = client.fetchMiningHotspots("LHS 495", "Monazite", 1, false);

        System.out.println("📊 Résultats trouvés: " + hotspots.size() + " hotspots");

        // Afficher tous les résultats avec détails
        hotspots
            .forEach(System.out::println
            );
    }

    public void searchSystem() throws IOException, InterruptedException {
        EdToolsClient client = new EdToolsClient();
        EdtoolResponse result = client.sendSystemSearch("Sol", 152, 1,true);

        java.lang.System.out.println("Rows: " + result.getRows().size());
        result.getRows().forEach(java.lang.System.out::println);

        EdtoolResponse result2 = client.sendTargetSystemSearch("Core Sys Sector OI-T b3-6");

        java.lang.System.out.println("Rows: " + result2.getRows().size());
        result2.getRows().forEach(java.lang.System.out::println);
    }
}
