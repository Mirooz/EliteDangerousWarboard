package be.mirooz.elitedangerous.lib.inara.client;

import be.mirooz.elitedangerous.lib.inara.model.Commodity;
import be.mirooz.elitedangerous.lib.inara.model.ConflictSystem;
import be.mirooz.elitedangerous.lib.inara.model.minerals.CoreMineral;
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
            "https://inara.cz";

    public List<ConflictSystem> fetchConflictSystems(String sourceSystem) throws IOException {
        String encodedSystem = URLEncoder.encode(sourceSystem, StandardCharsets.UTF_8);
        String url = BASE_URL + "/elite/nearest-misc/?pi20=1&ps1=" + encodedSystem;
        System.out.printf(
                "Calling INARA with parameters %s%n", sourceSystem);
        long start = System.currentTimeMillis();
        Document doc = Jsoup.connect(url)
                .header("User-Agent", "Java HttpClient - ED Warboard")
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

    /**
     * Récupère la liste des commodités depuis Inara selon les paramètres spécifiés
     * 
     * @param commodityName Nom de la commodité (ex: "Void Opal")
     * @param sourceSystem Système source pour la recherche (ex: "HIP 50694")
     * @param maxSystemDistance Distance maximale du système en années-lumière (ex: 50)
     * @param minLandingPad Taille minimale du pad d'atterrissage ("Small", "Medium", "Large")
     * @param maxStationDistance Distance maximale de la station en Ls (ex: 15000)
     * @param maxPriceAge Âge maximum des prix en jours (ex: 2)
     * @param minSupplyDemand Offre/demande minimale (ex: 1000)
     * @return Liste des commodités trouvées
     * @throws IOException en cas d'erreur de connexion
     */
    public List<Commodity> fetchCommoditiesAllArgs(CoreMineral coreMineral, String sourceSystem,
                                                   int maxSystemDistance, boolean largePad,
                                                   int maxStationDistance, int maxPriceAge,
                                                   int minSupplyDemand) throws IOException {
        
        // Encoder les paramètres pour l'URL
        String encodedSystem = URLEncoder.encode(sourceSystem, StandardCharsets.UTF_8);
        
        // Construire l'URL avec les paramètres
        StringBuilder urlBuilder = new StringBuilder(BASE_URL + "/elite/commodities/?formbrief=1");
        urlBuilder.append("&pi1=2"); // I want to sell
        urlBuilder.append("&pa1%5B%5D=").append(coreMineral.getInaraId()); // Commodity ID (à adapter selon la commodité)
        urlBuilder.append("&ps1=").append(encodedSystem); // Source system
        urlBuilder.append("&pi10=1"); // Include surface stations
        urlBuilder.append("&pi11=").append(maxSystemDistance); // Max system distance
        urlBuilder.append("&pi3=").append(largePad?"3":"2"); // Landing pad size (2 = Medium)
        urlBuilder.append("&pi9=").append(maxStationDistance); // Max station distance
        urlBuilder.append("&pi4=0"); // Include fleet carriers
        urlBuilder.append("&pi8=0"); // Include Stronghold carriers
        urlBuilder.append("&pi13=0"); // Power condition
        urlBuilder.append("&pi5=").append(maxPriceAge); // Max price age
        urlBuilder.append("&pi12=50"); // Price condition
        urlBuilder.append("&pi7=").append(minSupplyDemand); // Min supply/demand
        urlBuilder.append("&pi14=0"); // Powerplay state
        urlBuilder.append("&ps3="); // Minor faction (empty)
        
        String url = urlBuilder.toString();
        System.out.println("url \n" +url);
        System.out.printf("Calling INARA commodities with parameters: %s, %s%n", coreMineral.getInaraName(), sourceSystem);
        long start = System.currentTimeMillis();
        
        Document doc = Jsoup.connect(url)
                .header("User-Agent", "Java HttpClient - ED Warboard")
                .get();
        
        long durationCall = System.currentTimeMillis() - start;
        System.out.println("INARA commodities call duration: " + durationCall + " ms");
        
        List<Commodity> commodities = new ArrayList<>();
        
        // Chercher la table des résultats
        Element table = doc.selectFirst("table.tablesortercollapsed");
        if (table == null) {
            System.out.println("No commodities table found");
            // Debug: afficher toutes les tables disponibles
            Elements allTables = doc.select("table");
            System.out.println("Found " + allTables.size() + " tables total");
            for (int i = 0; i < Math.min(allTables.size(), 3); i++) {
                Element t = allTables.get(i);
                System.out.println("Table " + i + " classes: " + t.className());
            }
            return commodities;
        }
        
        Elements rows = table.select("tbody tr");
        System.out.println("Found " + rows.size() + " commodity rows");
        
        
        for (Element row : rows) {
            Elements cols = row.select("td");
            if (cols.size() < 6) {
                continue; // Vérifier qu'on a assez de colonnes
            }
            
            Commodity commodity = new Commodity();
            
            try {
                // Colonne 0: Station name et système
                Element stationElement = cols.get(0);
                String stationText = stationElement.text().trim();
                
                // Vérifier si c'est un Fleet Carrier en analysant les éléments HTML
                boolean isFleetCarrier = false;
                Elements stationIcons = stationElement.select(".stationicon");
                for (Element icon : stationIcons) {
                    String style = icon.attr("style");
                    if (style.contains("background-position: -507px")) {
                        isFleetCarrier = true;
                        break;
                    }
                }
                
                // Séparer station et système si possible
                String[] parts = stationText.split("\\|");
                if (parts.length >= 2) {
                    commodity.setStationName(parts[0].trim());
                    // Nettoyer tous les symboles spéciaux du nom de système
                    String systemName = cleanSpecialSymbols(parts[1].trim());
                    commodity.setSystemName(systemName);
                } else {
                    commodity.setStationName(stationText);
                }
                
                // Définir le statut Fleet Carrier
                commodity.setFleetCarrier(isFleetCarrier ? "Yes" : "No");
                
                // Colonne 1: Landing pad size
                if (cols.size() > 1) {
                    commodity.setLandingPadSize(cols.get(1).text().trim());
                }
                
                // Colonne 2: Station distance
                if (cols.size() > 2) {
                    String stationDist = cols.get(2).text().replace("Ls", "").trim();
                    commodity.setStationDistance(stationDist);
                }
                
                // Colonne 3: System distance
                if (cols.size() > 3) {
                    String systemDist = cleanSpecialSymbols(cols.get(3).text())
                        .replace("Ly", "")
                        .trim();
                    commodity.setSystemDistance(Double.parseDouble(systemDist));
                }
                
                // Colonne 4: Supply/Demand
                if (cols.size() > 4) {
                    String supplyText = cleanSpecialSymbols(cols.get(4).text()).trim();
                    commodity.setSupply(supplyText);
                }
                
                // Colonne 5: Price
                if (cols.size() > 5) {
                    String priceText = cols.get(5).text()
                        .replace("Cr", "")
                            .replace(",","")
                        .trim();
                    
                    // Gérer les prix avec min/max (ex: "480,335 - 891,144")
                    if (priceText.contains(" - ")) {
                        String[] priceParts = priceText.split(" - ");
                        if (priceParts.length == 2) {
                            String minPrice = priceParts[0].trim();
                            String maxPrice = priceParts[1].trim();
                            
                            commodity.setPriceMin(Integer.parseInt(minPrice));
                            commodity.setPriceMax(Integer.parseInt(maxPrice));
                            commodity.setPrice(Integer.parseInt(minPrice)); // Prix principal = prix minimum
                        } else {
                            commodity.setPrice(Integer.parseInt(priceText));
                        }
                    } else {
                        commodity.setPrice(Integer.parseInt(priceText));
                    }
                }
                
                // Colonne 6: Last update
                if (cols.size() > 6) {
                    commodity.setLastUpdate(cols.get(6).text().trim());
                }
                commodity.setCoreMineral(coreMineral);
                
                commodities.add(commodity);
                
            } catch (Exception e) {
                System.err.println("Error parsing commodity row: " + e.getMessage());
                continue;
            }
        }
        
        System.out.println("Successfully parsed " + commodities.size() + " commodities");
        return commodities;
    }

    /**
     * Nettoie les symboles spéciaux Unicode des chaînes de caractères
     * Garde seulement les lettres, chiffres, espaces et symboles de ponctuation courants
     * @param text Le texte à nettoyer
     * @return Le texte nettoyé
     */
    private String cleanSpecialSymbols(String text) {
        if (text == null) return "";
        // Garde seulement : lettres (a-z, A-Z), chiffres (0-9), espaces, tirets, points, virgules, parenthèses, crochets, plus, pipe
        return text.replaceAll("[^a-zA-Z0-9\\s\\-.,()\\[\\]|+\\s]", "").trim();
    }


    /**
     * Méthode simplifiée pour récupérer les commodités avec les paramètres de l'URL fournie
     * Utilise les paramètres par défaut correspondant à l'URL Inara fournie
     * 
     * Exemple d'utilisation :
     * <pre>
     * InaraClient client = new InaraClient();
     * List&lt;Commodity&gt; commodities = client.fetchCommoditiesSimple("Musgravite", "HIP 50694");
     * 
     * for (Commodity commodity : commodities) {
     *     System.out.println("Station: " + commodity.getStationName());
     *     System.out.println("Système: " + commodity.getSystemName());
     *     System.out.println("Prix: " + commodity.getPrice());
     *     System.out.println("Distance: " + commodity.getSystemDistance() + " Ly");
     * }
     * </pre>
     * 
     * @param commodityName Nom de la commodité (ex: "Musgravite")
     * @param sourceSystem Système source (ex: "HIP 50694")
     * @return Liste des commodités trouvées
     * @throws IOException en cas d'erreur de connexion
     */
    public List<Commodity> fetchMinerMarket(CoreMineral coreMineral, String sourceSystem, int distance, int supplyDemand, boolean largePad) throws IOException {
        // Trouver l'ID correspondant au nom
        return fetchCommoditiesAllArgs(
                coreMineral,
            sourceSystem,
            distance,        // maxSystemDistance: ~ 100 Ly
            largePad,  // minLandingPad: Medium
            15000,     // maxStationDistance: 15000 Ls
            48,         // maxPriceAge: 48 days
            supplyDemand       // minSupplyDemand: 500 Units
        );
    }
}
