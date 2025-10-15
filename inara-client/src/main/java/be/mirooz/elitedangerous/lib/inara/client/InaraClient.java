package be.mirooz.elitedangerous.lib.inara.client;

import be.mirooz.elitedangerous.lib.inara.model.commodities.InaraCommoditiesStats;
import be.mirooz.elitedangerous.lib.inara.model.commodities.minerals.CoreMineralType;
import be.mirooz.elitedangerous.lib.inara.model.conflictsearch.ConflictSystem;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InaraClient {

    private static final String BASE_URL = "https://inara.cz";
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    // ─────────────────────────────────────────────
    // ⚔️ FETCH CONFLICT SYSTEMS
    // ─────────────────────────────────────────────
    public List<ConflictSystem> fetchConflictSystems(String sourceSystem) throws IOException {
        String encodedSystem = URLEncoder.encode(sourceSystem, StandardCharsets.UTF_8);
        String url = BASE_URL + "/elite/nearest-misc/?pi20=1&ps1=" + encodedSystem;
        System.out.printf("Calling INARA (conflict systems) with parameters %s%n", sourceSystem);
        long start = System.currentTimeMillis();

        Document doc = fetchHtml(url);

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
            String systemName = cols.get(0).text().replace("︎", "").trim();
            system.setSystemName(systemName);
            String conflictCount = cols.get(1).text().trim();
            system.setSurfaceConflicts(conflictCount.isEmpty() ? 0 : Integer.parseInt(conflictCount));
            system.setFaction(cols.get(2).text().trim());
            system.setOpponentFaction(cols.get(3).text().trim());

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

    // ─────────────────────────────────────────────
    // 💰 FETCH COMMODITIES
    // ─────────────────────────────────────────────
    public List<InaraCommoditiesStats> fetchCommoditiesAllArgs(CoreMineralType coreMineral, String sourceSystem,
                                                               int maxSystemDistance, boolean largePad,
                                                               int maxStationDistance, int maxPriceAge,
                                                               int minSupplyDemand) throws IOException {

        // Encoder les paramètres pour l'URL
        String encodedSystem = URLEncoder.encode(sourceSystem, StandardCharsets.UTF_8);

        String url = BASE_URL + "/elite/commodities/?formbrief=1"
                + "&pi1=2"
                + "&pa1%5B%5D=" + coreMineral.getInaraId()
                + "&ps1=" + encodedSystem
                + "&pi10=1"
                + "&pi11=" + maxSystemDistance
                + "&pi3=" + (largePad ? "3" : "2")
                + "&pi9=" + maxStationDistance
                + "&pi4=0"
                + "&pi8=0"
                + "&pi13=0"
                + "&pi5=" + maxPriceAge
                + "&pi12=50"
                + "&pi7=" + minSupplyDemand
                + "&pi14=0"
                + "&ps3=";

        System.out.println("url \n" + url);
        System.out.printf("Calling INARA commodities with parameters: %s, %s%n", coreMineral.getInaraName(), sourceSystem);

        long start = System.currentTimeMillis();
        Document doc = fetchHtml(url);
        long durationCall = System.currentTimeMillis() - start;
        System.out.println("INARA commodities call duration: " + durationCall + " ms");

        List<InaraCommoditiesStats> commodities = new ArrayList<>();
        Element table = doc.selectFirst("table.tablesortercollapsed");
        if (table == null) {
            System.out.println("No commodities table found");
            Elements allTables = doc.select("table");
            System.out.println("Found " + allTables.size() + " tables total");
            return commodities;
        }

        Elements rows = table.select("tbody tr");
        System.out.println("Found " + rows.size() + " commodity rows");

        for (Element row : rows) {
            Elements cols = row.select("td");
            if (cols.size() < 6) continue;

            try {
                InaraCommoditiesStats stats = parseCommoditiesRow(cols, coreMineral);
                commodities.add(stats);
            } catch (Exception e) {
                System.err.println("Error parsing inaraCommoditiesStats row: " + e.getMessage());
            }
        }

        System.out.println("Successfully parsed " + commodities.size() + " commodities");
        return commodities;
    }

    private InaraCommoditiesStats parseCommoditiesRow(Elements cols, CoreMineralType coreMineral) {
        InaraCommoditiesStats stats = new InaraCommoditiesStats();

        // Station name + system
        Element stationElement = cols.get(0);
        String stationText = stationElement.text().trim();
        Element stationNameElement = stationElement.selectFirst("span.standardcase");
        stats.setStationName(stationNameElement != null ? stationNameElement.text().trim() : stationText);

        Element systemNameElement = stationElement.selectFirst("span.uppercase.nowrap");
        if (systemNameElement != null) {
            stats.setSystemName(cleanSpecialSymbols(systemNameElement.text().trim()));
        }

        // Landing pad
        stats.setLandingPadSize(cols.get(1).text().trim());

        // Station distance
        stats.setStationDistance(cols.get(2).text().replace("Ls", "").trim());

        // System distance
        String systemDist = cleanSpecialSymbols(cols.get(3).text()).replace("Ly", "").trim();
        stats.setSystemDistance(Double.parseDouble(systemDist));

        // Demand
        String demandText = cleanSpecialSymbols(cols.get(4).text()).replace(",", "").trim();
        stats.setDemand(Integer.parseInt(demandText));

        // Price
        String priceText = cols.get(5).text().replace("Cr", "").replace(",", "").trim();
        if (priceText.contains(" - ")) {
            String[] priceParts = priceText.split(" - ");
            stats.setPriceMin(Integer.parseInt(priceParts[0].trim()));
            stats.setPriceMax(Integer.parseInt(priceParts[1].trim()));
            stats.setPrice(Integer.parseInt(priceParts[0].trim()));
        } else {
            stats.setPrice(Integer.parseInt(priceText));
        }

        // Last update
        if (cols.size() > 6) {
            stats.setLastUpdate(cols.get(6).text().trim());
        }

        stats.setCoreMineral(coreMineral);
        return stats;
    }

    // ─────────────────────────────────────────────
    // 🌐 FETCH HTML AVEC HTTPCLIENT
    // ─────────────────────────────────────────────
    private Document fetchHtml(String url) throws IOException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Java HttpClient - ED Warboard")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Status code: " + response.statusCode());
            System.out.println("Headers: " + response.headers().map());

            if (response.statusCode() == 429) {
                response.headers().firstValue("Retry-After").ifPresent(ra ->
                        System.out.println("Retry-After (sec): " + ra)
                );
            }

            return Jsoup.parse(response.body());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("HTTP request interrupted", e);
        }
    }

    private String cleanSpecialSymbols(String text) {
        if (text == null) return "";
        return text.replaceAll("[^a-zA-Z0-9\\s\\-.,()\\[\\]|+\\s]", "").trim();
    }

    public List<InaraCommoditiesStats> fetchMinerMarket(CoreMineralType coreMineral, String sourceSystem, int distance, int supplyDemand, boolean largePad) throws IOException {
        return fetchCommoditiesAllArgs(coreMineral, sourceSystem, distance, largePad, 15000, 48, supplyDemand);
    }
}
