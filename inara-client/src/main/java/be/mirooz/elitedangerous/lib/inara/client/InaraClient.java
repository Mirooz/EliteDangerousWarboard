package be.mirooz.elitedangerous.lib.inara.client;

import be.mirooz.elitedangerous.lib.inara.model.ConflictSystem;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class InaraClient {

    private static final String BASE_URL =
            "https://inara.cz/elite/nearest-misc/?pi20=1&ps1=";

    public List<ConflictSystem> fetchConflictSystems(String sourceSystem) throws IOException {
        String encodedSystem = URLEncoder.encode(sourceSystem, StandardCharsets.UTF_8);
        String url = BASE_URL + encodedSystem;
        System.out.printf(
                "Calling INARA with parameters %s%n", sourceSystem);
        long start = System.currentTimeMillis();
        Document doc = Jsoup.connect(url)
                .header("User-Agent", "Java HttpClient - ED Dashboard")
                .get();

        long durationCall = System.currentTimeMillis() - start;
        System.out.println("INARA call duration: " + durationCall + " ms");
        Element table = doc.selectFirst("table.tablesortercollapsed");

        List<ConflictSystem> systems = new ArrayList<>();
        if (table == null) return systems;

        Elements rows = table.select("tbody tr");
        for (Element row : rows) {
            Elements cols = row.select("td");
            if (cols.size() < 5) continue;

            ConflictSystem system = new ConflictSystem();

            // 1️⃣ System name → nettoyer les symboles "︎"
            String systemName = cols.get(0).text().replace("︎", "").trim();
            system.setSystemName(systemName);

            // 2️⃣ Surface conflicts
            String conflictCount = cols.get(1).text().trim();
            system.setSurfaceConflicts(conflictCount.isEmpty() ? 0 : Integer.parseInt(conflictCount));

            // 3️⃣ Faction
            system.setFaction(cols.get(2).text().trim());

            // 4️⃣ Opponent
            system.setOpponentFaction(cols.get(3).text().trim());

            // 5️⃣ Distance → nettoyer "Ly︎"
            String distanceText = cols.get(4).text().replace("Ly", "").replace("︎", "").trim();
            try {
                system.setDistanceLy(Double.parseDouble(distanceText));
            } catch (NumberFormatException e) {
                system.setDistanceLy(0);
            }
            systems.add(system);
        }

        return systems;
    }
}
