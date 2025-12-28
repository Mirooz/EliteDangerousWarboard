package be.mirooz.ardentapi.client;

import be.mirooz.ardentapi.model.CommodityMaxSell;
import be.mirooz.ardentapi.model.CommoditiesStats;
import be.mirooz.ardentapi.model.StationMarket;
import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.Mineral;
import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.MineralType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ArdentApiClient {
    private static final String BASE_URL = "https://api.ardent-insight.com";
    private static final HttpClient httpClient = HttpClient.newHttpClient();


    /**
     * Récupère la liste des commodités depuis Inara et extrait le prix max de vente de chaque commodité
     *
     * @return Une liste de CommodityMaxSell avec le nom de la commodité et son prix max de vente
     * @throws IOException Si une erreur se produit lors de l'appel HTTP
     */
    public List<CommodityMaxSell> fetchCommoditiesMaxSell() throws IOException {
        String url = BASE_URL + "/v2/commodities";
        System.out.println("Fetching commodities list from: " + url);

        HttpResponse<String> response = fetchHtml(url);
        ObjectMapper mapper = new ObjectMapper();

        return mapper.readValue(response.body(),
                new TypeReference<>() {
                });
    }
    public List<CommoditiesStats> fetchMinerMarket(Mineral coreMineral, String sourceSystem, int distance, int supplyDemand, boolean largePad, boolean fleetCarrier) throws IOException {
        return fetchCommoditiesAllArgs(coreMineral, sourceSystem, distance, largePad, fleetCarrier, 15000, 48, supplyDemand);
    }

    public StationMarket fetchStationMarket(String marketId) throws IOException {

        String url = BASE_URL + "/v2/market/" + marketId + "/commodities";
        System.out.println("Fetching station market from: " + url);

        HttpResponse<String> response = fetchHtml(url);
        ObjectMapper mapper = new ObjectMapper();

        List<JsonNode> rows =
                mapper.readValue(response.body(), new TypeReference<>() {});

        if (rows.isEmpty()) {
            return null;
        }

        // ─────────────────────────────────────────────
        // 1️⃣ Initialisation du StationMarket (1ère ligne)
        // ─────────────────────────────────────────────
        JsonNode first = rows.get(0);

        StationMarket market = new StationMarket();
        market.setStationName(first.path("stationName").asText());
        market.setSystemName(first.path("systemName").asText());
        market.setStationDistance(first.path("distanceToArrival").asText());
        market.setLandingPadSize(String.valueOf(first.path("maxLandingPadSize").asInt()));
        market.setMarketUpdate(first.path("updatedAt").asText());
        market.setStationUpdate(first.path("updatedAt").asText());
        market.setLocationUpdate(first.path("updatedAt").asText());

        // optionnel
        market.setStationUrl("https://inara.cz/market/" + marketId);

        // ─────────────────────────────────────────────
        // 2️⃣ Commodities
        // ─────────────────────────────────────────────
        for (JsonNode row : rows) {
            StationMarket.CommodityMarketEntry entry =
                    new StationMarket.CommodityMarketEntry();

            String commodityName = row.path("commodityName").asText();

            entry.setCommodityName(commodityName);
            entry.setSellPrice(row.path("sellPrice").asInt());
            entry.setBuyPrice(row.path("buyPrice").asInt());
            entry.setDemand(row.path("demand").asInt());
            entry.setSupply(row.path("stock").asInt());

            // heuristiques simples
            int meanPrice = row.path("meanPrice").asInt();
            entry.setBetterThanAverage(entry.getSellPrice() > meanPrice);
            entry.setBestPrice(false); // à calculer plus tard si besoin

            // mapping métier ICommodity
            entry.setCommodity(
                    MineralType
                            .fromCargoJsonName(commodityName)
                            .orElse(null)
            );

            market.getCommodities().add(entry);
        }

        return market;
    }

    private List<CommoditiesStats> fetchCommoditiesAllArgs(
            Mineral coreMineral,
            String sourceSystem,
            int maxSystemDistance,
            boolean largePad,
            boolean fleetCarrier,
            int maxStationDistance,
            int maxPriceAge,
            int minSupplyDemand
    ) throws IOException {

        StringBuilder url = new StringBuilder(BASE_URL)
                .append("/v2/system/name/")
                .append(encode(sourceSystem))
                .append("/commodity/name/")
                .append(coreMineral.getCargoJsonName().toLowerCase())
                .append("/nearby/imports")
                .append("?minVolume=").append(minSupplyDemand)
                .append("&maxDistance=").append(Math.min(maxSystemDistance, 500))
                .append("&maxDaysAgo=").append(maxPriceAge)
                .append("&minPrice=1");

        if (!fleetCarrier) {
            url.append("&fleetCarriers=false");
        }

        System.out.println("Fetching imports from: " + url);

        HttpResponse<String> response = fetchHtml(url.toString());
        ObjectMapper mapper = new ObjectMapper();

        List<CommoditiesStats> stats =
                mapper.readValue(response.body(), new TypeReference<>() {});

        // 🔍 Post-filtrage Java (paramètres non supportés par l'API)
        return stats.stream()
                // distance station
                .filter(s -> maxStationDistance <= 0
                        || parseDoubleSafe(s.getStationDistance()) <= maxStationDistance)

                // large pad uniquement
                .filter(s -> !largePad || "3".equals(s.getLandingPadSize()))

                // fleet carrier si demandé (filtre dans l'url)
                //.filter(s -> fleetCarrier || !s.isFleetCarrier())

                // sécurité
                .filter(s -> s.getPrice() > 0)

                // tri par prix décroissant
                .sorted((a, b) -> Integer.compare(b.getPrice(), a.getPrice()))

                .toList();
    }

    private HttpResponse<String> fetchHtml(String url) throws IOException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Java HttpClient - ED Warboard")
                    .GET()
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());


            System.out.println("Status code: " + response.statusCode());
            System.out.println("Headers: " + response.headers().map());

            if (response.statusCode() == 429) {
                response.headers().firstValue("Retry-After").ifPresent(ra ->
                        System.out.println("Retry-After (sec): " + ra)
                );
            }

            return response;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("HTTP request interrupted", e);
        }
    }
    private String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8)
                    .replace("+", "%20");
        } catch (Exception e) {
            return value;
        }
    }


    private double parseDoubleSafe(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return Double.MAX_VALUE;
        }
    }
}
