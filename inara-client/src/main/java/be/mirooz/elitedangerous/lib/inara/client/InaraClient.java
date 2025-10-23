package be.mirooz.elitedangerous.lib.inara.client;

import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.Mineral;
import be.mirooz.elitedangerous.lib.inara.model.InaraCommoditiesStats;
import be.mirooz.elitedangerous.commons.lib.models.commodities.ICommodity;
import be.mirooz.elitedangerous.commons.lib.models.commodities.ICommodityFactory;
import be.mirooz.elitedangerous.lib.inara.model.StationMarket;
import be.mirooz.elitedangerous.lib.inara.model.StationType;
import be.mirooz.elitedangerous.lib.inara.model.conflictsearch.ConflictSystem;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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


    public List<InaraCommoditiesStats> fetchCommoditiesAllArgs(Mineral coreMineral, String sourceSystem,
                                                               int maxSystemDistance, boolean largePad, boolean fleetCarrier,
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
                + "&pi8=" + (fleetCarrier ? "0" : "1")
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

    private InaraCommoditiesStats parseCommoditiesRow(Elements cols, Mineral mineral) {
        InaraCommoditiesStats stats = new InaraCommoditiesStats();

        // Station name + system
        Element stationElement = cols.get(0);
        String stationText = stationElement.text().replace("|", "").trim();
        Element stationNameElement = stationElement.selectFirst("span.standardcase");
        stats.setStationName(stationNameElement != null ? stationNameElement.text().replace("|", "").trim() : stationText);
        
        // Extraire l'URL de la station depuis le lien href
        Element stationLink = stationElement.selectFirst("a[href]");
        if (stationLink != null) {
            String href = stationLink.attr("href");
            stats.setStationUrl(href);
        }
        
        boolean isFleetCarrier = false;
        Elements stationIcons = stationElement.select(".stationicon");
        StationType stationType = StationType.CORIOLIS;
        for (Element icon : stationIcons) {
            String style = icon.attr("style");
            if (style.contains("background-position: -507px")) {
                isFleetCarrier = true;
                stationType = StationType.FLEET;
                break;
            } else if (style.contains("background-position: -26px")) {
                stationType = StationType.PORT;
                break;
            }
        }

        stats.setFleetCarrier(isFleetCarrier);
        stats.setStationType(stationType);
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

        stats.setMineral(mineral);
        return stats;
    }

    // ─────────────────────────────────────────────
    // 🏪 FETCH STATION MARKET
    // ─────────────────────────────────────────────
    public StationMarket fetchStationMarket(String stationUrl) throws IOException {
        String url = BASE_URL + stationUrl;
        System.out.printf("Calling INARA (station market) with URL: %s%n", url);
        long start = System.currentTimeMillis();

        Document doc = fetchHtml(url);

        long durationCall = System.currentTimeMillis() - start;
        System.out.println("INARA station market call duration: " + durationCall + " ms");

        StationMarket market = new StationMarket();
        
        // Extraire les informations de la station depuis le header
        parseStationHeader(doc, market);
        
        // Extraire les commodités depuis le tableau
        parseCommoditiesTable(doc, market);

        System.out.println("Successfully parsed station market with " + market.getCommodities().size() + " commodities");
        return market;
    }
    
    /**
     * Parse les informations de la station depuis le header de la page
     */
    private void parseStationHeader(Document doc, StationMarket market) {
        // Nom de la station et URL
        Element stationLink = doc.selectFirst("h2 a[href]");
        if (stationLink != null) {
            market.setStationUrl(stationLink.attr("href"));
            String stationText = stationLink.text().trim();
            // Extraire le nom de la station (avant le code entre parenthèses)
            if (stationText.contains("(")) {
                market.setStationName(stationText.substring(0, stationText.indexOf("(")).trim());
            } else {
                market.setStationName(stationText);
            }
        }
        
        // Nom du système
        Element systemLink = doc.selectFirst("h2 a[href*='starsystem']");
        if (systemLink != null) {
            market.setSystemName(systemLink.text().trim());
        }
        
        // Informations de la station depuis les itempaircontainer
        Elements infoContainers = doc.select(".itempaircontainer");
        for (Element container : infoContainers) {
            Element label = container.selectFirst(".itempairlabel");
            Element value = container.selectFirst(".itempairvalue");
            
            if (label != null && value != null) {
                String labelText = label.text().trim();
                String valueText = value.text().trim();
                
                switch (labelText) {
                    case "Station distance":
                        market.setStationDistance(valueText);
                        break;
                    case "Landing pad":
                        market.setLandingPadSize(valueText);
                        break;
                    case "Market update":
                        market.setMarketUpdate(valueText);
                        break;
                    case "Station update":
                        market.setStationUpdate(valueText);
                        break;
                    case "Location update":
                        market.setLocationUpdate(valueText);
                        break;
                }
            }
        }
    }
    
    /**
     * Parse le tableau des commodités
     */
    private void parseCommoditiesTable(Document doc, StationMarket market) {
        Element table = doc.selectFirst("table.tablesortercollapsed");
        if (table == null) {
            System.out.println("No commodities table found in station market");
            return;
        }

        Elements rows = table.select("tbody tr");
        System.out.println("Found " + rows.size() + " commodity rows in station market");

        String currentCategory = "";
        
        for (Element row : rows) {
            // Vérifier si c'est une ligne de sous-en-tête (catégorie)
            if (row.hasClass("subheader")) {
                Element categoryCell = row.selectFirst("td.subheader");
                if (categoryCell != null) {
                    currentCategory = categoryCell.text().trim();
                    System.out.println("Found category: " + currentCategory);
                }
                continue;
            }
            
            Elements cols = row.select("td");
            if (cols.size() < 5) continue;

            try {
                StationMarket.CommodityMarketEntry entry = parseCommodityMarketRow(cols, currentCategory);
                market.getCommodities().add(entry);
            } catch (Exception e) {
                System.err.println("Error parsing commodity market row: " + e.getMessage());
            }
        }
    }
    
    /**
     * Parse une ligne de commodité du marché de station
     */
    private StationMarket.CommodityMarketEntry parseCommodityMarketRow(Elements cols, String category) {
        StationMarket.CommodityMarketEntry entry = new StationMarket.CommodityMarketEntry();
        entry.setCategory(category);

        // Nom de la commodité et URL
        Element commodityLink = cols.get(0).selectFirst("a[href]");
        if (commodityLink != null) {
            entry.setCommodityName(commodityLink.text().trim());
            String commodityUrl = commodityLink.attr("href");
            entry.setCommodityUrl(commodityUrl);
            
            // Extraire l'ID Inara de l'URL et créer l'objet ICommodity
            String inaraId = extractInaraIdFromUrl(commodityUrl);
            if (inaraId != null) {
                ICommodity commodity = ICommodityFactory.ofByInaraId(inaraId).orElse(null);
                entry.setCommodity(commodity);
                if (commodity != null) {
                    System.out.printf("🔗 Commodité trouvée: %s (ID: %s)%n", commodity.getInaraName(), inaraId);
                } else {
                    System.out.printf("❌ Commodité non trouvée pour l'ID: %s%n", inaraId);
                }
            } else {
                System.out.printf("❌ Impossible d'extraire l'ID Inara de l'URL: %s%n", commodityUrl);
            }
        }
        
        // Prix de vente
        Element sellPriceElement = cols.get(1).selectFirst(".marketpricelowdiff, .marketpricehighdiff");
        if (sellPriceElement != null) {
            String priceText = sellPriceElement.text().replace("Cr", "").replace(",", "").trim();
            try {
                entry.setSellPrice(Integer.parseInt(priceText));
            } catch (NumberFormatException e) {
                entry.setSellPrice(0);
            }
            
            // Déterminer si c'est le meilleur prix ou mieux que la moyenne
            if (sellPriceElement.hasClass("marketpricehighdiff")) {
                entry.setBestPrice(true);
            } else if (sellPriceElement.hasClass("marketpricelowdiff")) {
                entry.setBetterThanAverage(true);
            }
        }
        
        // Demande
        String demandText = cols.get(2).text().replace(",", "").trim();
        try {
            entry.setDemand(Integer.parseInt(demandText));
        } catch (NumberFormatException e) {
            entry.setDemand(0);
        }
        
        // Prix d'achat
        Element buyPriceElement = cols.get(3).selectFirst(".marketpricelowdiff, .marketpricehighdiff");
        if (buyPriceElement != null) {
            String priceText = buyPriceElement.text().replace("Cr", "").replace(",", "").trim();
            try {
                entry.setBuyPrice(Integer.parseInt(priceText));
            } catch (NumberFormatException e) {
                entry.setBuyPrice(0);
            }
        } else {
            // Si pas de prix d'achat, vérifier si c'est "-"
            String buyText = cols.get(3).text().trim();
            if (!"-".equals(buyText)) {
                try {
                    entry.setBuyPrice(Integer.parseInt(buyText.replace(",", "").trim()));
                } catch (NumberFormatException e) {
                    entry.setBuyPrice(0);
                }
            }
        }
        
        // Offre
        String supplyText = cols.get(4).text().replace(",", "").trim();
        try {
            entry.setSupply(Integer.parseInt(supplyText));
        } catch (NumberFormatException e) {
            entry.setSupply(0);
        }
        
        return entry;
    }

    /**
     * Extrait l'ID Inara d'une URL de commodité
     * Exemple: "/elite/commodity/81/" -> "81"
     * Exemple: "/elite/commodities/?commodity=81" -> "81"
     * 
     * @param url L'URL de la commodité
     * @return L'ID Inara ou null si non trouvé
     */
    private String extractInaraIdFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        
        // Format 1: /elite/commodity/81/
        if (url.contains("/elite/commodity/")) {
            Pattern pattern = Pattern.compile("/elite/commodity/(\\d+)/");
            Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        
        // Format 2: /elite/commodities/?commodity=81
        if (url.contains("commodity=")) {
            String[] parts = url.split("commodity=");
            if (parts.length > 1) {
                String commodityPart = parts[1];
                // Prendre seulement les chiffres au début
                StringBuilder id = new StringBuilder();
                for (char c : commodityPart.toCharArray()) {
                    if (Character.isDigit(c)) {
                        id.append(c);
                    } else {
                        break;
                    }
                }
                return id.length() > 0 ? id.toString() : null;
            }
        }
        
        return null;
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


    public List<InaraCommoditiesStats> fetchMinerMarket(Mineral coreMineral, String sourceSystem, int distance, int supplyDemand, boolean largePad, boolean fleetCarrier) throws IOException {
        return fetchCommoditiesAllArgs(coreMineral, sourceSystem, distance, largePad, fleetCarrier, 15000, 48, supplyDemand);
    }
}
