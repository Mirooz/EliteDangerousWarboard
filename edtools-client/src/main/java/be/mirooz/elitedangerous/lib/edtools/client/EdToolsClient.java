// src/main/java/be/mirooz/elitedangerous/edtools/client/EdToolsPveClient.java
package be.mirooz.elitedangerous.lib.edtools.client;

import be.mirooz.elitedangerous.lib.edtools.model.EdtoolResponse;
import be.mirooz.elitedangerous.lib.edtools.model.MassacreSystem;
import be.mirooz.elitedangerous.lib.edtools.model.MiningHotspot;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class EdToolsClient {

    private final Cache<String, List<MiningHotspot>> miningHotspotCache =
            Caffeine.newBuilder()
                    .expireAfterWrite(30, TimeUnit.MINUTES)
                    .maximumSize(500)
                    .build();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    public EdtoolResponse sendTargetSystemSearch(String referenceSystem) throws IllegalArgumentException, IOException, InterruptedException {
        String s = URLEncoder.encode(referenceSystem, StandardCharsets.UTF_8);
        String baseUrl = String.format(
                "https://edtools.cc/pve?s=%s&a=rt",s);
        return fetchSystemsSearch(referenceSystem, 0, 0, baseUrl);
    }

    public EdtoolResponse sendSystemSearch(String referenceSystem, int maxDistanceLy, int minSourcesPerTarget,boolean largePad) throws IllegalArgumentException, IOException, InterruptedException {
        validateParams(maxDistanceLy, minSourcesPerTarget);
        String s = URLEncoder.encode(referenceSystem, StandardCharsets.UTF_8);
        String baseUrl = String.format(
                "https://edtools.cc/pve?s=%s&md=%d&sc=%d", s, maxDistanceLy, minSourcesPerTarget);
        if (largePad) {
            baseUrl += "&lo=on";
        }

        return fetchSystemsSearch(referenceSystem, maxDistanceLy, minSourcesPerTarget, baseUrl);
    }

    private EdtoolResponse fetchSystemsSearch(String referenceSystem, int maxDistanceLy, int minSourcesPerTarget, String baseUrl) throws IOException, InterruptedException {
        String url = baseUrl;
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "Java HttpClient - ED Dashboard")
                .GET().build();
        System.out.printf(
                "Calling EDTOOLS with parameters %s, %s, %s%n",
                request, maxDistanceLy, minSourcesPerTarget
        );
        long start = System.currentTimeMillis();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        long durationCall = System.currentTimeMillis() - start;
        System.out.println("EDTOOLS call duration: " + durationCall + " ms");
        if (response.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + response.statusCode() + " calling " + url);
        }

        List<MassacreSystem> rows = parseTable(response.body());
        return new EdtoolResponse(referenceSystem, maxDistanceLy, minSourcesPerTarget, rows);
    }

    private void validateParams(int maxDistanceLy, int minSourcesPerTarget) {
        if (maxDistanceLy < 30 || maxDistanceLy > 250) {
            throw new IllegalArgumentException("maxDistanceLy doit être entre 30 et 250 (valeur: " + maxDistanceLy + ")");
        }
        if (minSourcesPerTarget < 1 || minSourcesPerTarget > 5) {
            throw new IllegalArgumentException("minSourcesPerTarget doit être entre 1 et 5 (valeur: " + minSourcesPerTarget + ")");
        }
    }
    private List<MassacreSystem> parseTable(String html) {
        Document doc = Jsoup.parse(html);
        Element table = doc.selectFirst("table#sys_tbl");
        if (table == null) return List.of();

        List<MassacreSystem> rows = new ArrayList<>();
        for (Element tr : table.select("tr")) {
            Elements tds = tr.select("td");
            if (tds.size() < 11) continue; // skip header/invalid rows

            String distance = tds.get(0).text().trim();

            // Source system: 2e cellule, lien avec id="cpb_x"
            String sourceSystem = Optional.ofNullable(tds.get(1).selectFirst("a[id^=cpb_]"))
                    .map(Element::text).orElse(tds.get(1).text()).trim();

            String lPad = tds.get(2).text().trim();
            String mPad = tds.get(3).text().trim();
            String pPad = tds.get(4).text().trim();
            String fed  = tds.get(5).text().trim();
            String imp  = tds.get(6).text().trim();
            String all  = tds.get(7).text().trim();
            String ind  = tds.get(8).text().trim();

            String cellText = tds.get(9).text().trim();
            String targetSystem = cellText.replaceAll("/.*", "").trim();
            String targetCount = cellText.replaceAll(".*\\[\\s*(\\d+)\\s*\\].*", "$1");
            String resRings = tds.get(10).text().trim();

            rows.add(new MassacreSystem(distance, sourceSystem, lPad, mPad, pPad, fed, imp, all, ind, targetSystem,targetCount, resRings));
        }
        return rows;
    }

    
    /**
     * Récupère les hotspots de minage depuis edtools.cc
     * 
     * @param referenceSystem Système de référence (ex: "LHS 495")
     * @param mineralName Nom du minéral (ex: "Monazite")
     * @param minHotspots Nombre minimum de hotspots (ex: 1)
     * @param populatedOnly Systèmes peuplés uniquement (ex: false)
     * @return Liste des hotspots trouvés
     * @throws IOException en cas d'erreur de connexion
     */
    public List<MiningHotspot> fetchMiningHotspots(String referenceSystem, String mineralName,
                                                   int minHotspots, boolean populatedOnly) throws IOException {
        
        // Construire l'URL
        StringBuilder urlBuilder = new StringBuilder("https://edtools.cc/hotspot");
        urlBuilder.append("?s=").append(URLEncoder.encode(referenceSystem, StandardCharsets.UTF_8));
        urlBuilder.append("&m=").append(URLEncoder.encode(mineralName, StandardCharsets.UTF_8));
        urlBuilder.append("&ms=").append(minHotspots);
        if (populatedOnly) {
            urlBuilder.append("&pop=on");
        }
        
        String url = urlBuilder.toString();
        System.out.println("🔍 Appel EDTools hotspots: " + url);
        long start = System.currentTimeMillis();
        
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "Java HttpClient - ED Dashboard")
                .GET().build();
        
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interruption lors de l'appel EDTools", e);
        }
        
        long durationCall = System.currentTimeMillis() - start;
        System.out.println("⏱️ Durée appel EDTools: " + durationCall + " ms");
        
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " calling " + url);
        }
        
        return parseMiningHotspots(response.body());
    }

    /**
     * Méthode simplifiée avec des paramètres par défaut
     *
     * @param referenceSystem Système de référence
     * @param mineralName Nom du minéral
     * @return Liste des hotspots trouvés
     * @throws IOException en cas d'erreur de connexion
     */
    public List<MiningHotspot> fetchMiningHotspots(String referenceSystem, String mineralName) throws IOException {
        String key = referenceSystem + "|" + mineralName;
        return miningHotspotCache.get(key, value -> {
            try {
                return fetchMiningHotspots(referenceSystem, mineralName, 1, false);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    
    private List<MiningHotspot> parseMiningHotspots(String html) {
        Document doc = Jsoup.parse(html);
        List<MiningHotspot> hotspots = new ArrayList<>();
        
        // Chercher la table des résultats
        Element table = doc.selectFirst("table#sys_tbl");
        if (table == null) {
            System.out.println("❌ Aucune table de hotspots trouvée");
            return hotspots;
        }
        
        Elements rows = table.select("tbody tr");
        System.out.println("📊 Trouvé " + rows.size() + " lignes de hotspots");
        
        for (Element row : rows) {
            Elements cols = row.select("td");
            if (cols.size() < 6) {
                continue;
            }
            
            try {
                MiningHotspot hotspot = new MiningHotspot();
                
                // Colonne 0: Distance
                String distanceText = cols.get(0).text().trim();
                hotspot.setDistanceFromReference(Double.parseDouble(distanceText));
                
                // Colonne 1: System
                Element systemElement = cols.get(1);
                String systemText = systemElement.text().trim();
                // Enlever l'astérisque (*) si présent
                systemText = systemText.replace("*", "").trim();
                hotspot.setSystemName(systemText);
                
                // Colonne 2: Ring - avec tooltip contenant les détails des minéraux
                Element ringElement = cols.get(2);
                
                // Extraire seulement le nom de l'anneau (sans le contenu du tooltip)
                Element ringNameElement = ringElement.selectFirst("span.hvr");
                String ringText;
                if (ringNameElement != null) {
                    // Prendre seulement le texte avant le span.ttip
                    String fullText = ringNameElement.text();
                    Element tooltipElement = ringNameElement.selectFirst("span.ttip");
                    if (tooltipElement != null) {
                        String tooltipText = tooltipElement.text();
                        ringText = fullText.replace(tooltipText, "").trim();
                    } else {
                        ringText = fullText.trim();
                    }
                } else {
                    ringText = ringElement.text().trim();
                }
                hotspot.setRingName(ringText);
                
                // Extraire les détails des minéraux depuis le tooltip
                Element tooltip = ringElement.selectFirst("span.ttip");
                if (tooltip != null) {
                    String tooltipHtml = tooltip.html().trim();
                    // Parser le format "MineralName:Count" avec <br> comme séparateur
                    String[] mineralEntries = tooltipHtml.split("<br>");
                    for (String entry : mineralEntries) {
                        entry = entry.trim();
                        if (entry.contains(":")) {
                            String[] parts = entry.split(":");
                            if (parts.length == 2) {
                                try {
                                    String mineral = parts[0].trim();
                                    int count = Integer.parseInt(parts[1].trim());
                                    hotspot.addMineral(mineral, count);
                                } catch (NumberFormatException e) {
                                    System.err.println("❌ Erreur parsing minéral: " + entry);
                                }
                            }
                        }
                    }
                } else {
                    System.out.println("❌ Aucun tooltip trouvé pour: " + ringText);
                }

                // Colonne 3: Type
                String typeText = cols.get(3).text().trim();
                hotspot.setRingType(typeText);
                
                // Colonne 4: Hotspots
                String hotspotsText = cols.get(4).text().trim();
                hotspot.setHotspotCount(Integer.parseInt(hotspotsText));
                
                // Colonne 5: LS
                String lsText = cols.get(5).text().trim();
                hotspot.setLightSeconds(Integer.parseInt(lsText.replace(",", "")));
                
                hotspots.add(hotspot);
                
            } catch (Exception e) {
                System.err.println("❌ Erreur parsing ligne hotspot: " + e.getMessage());
                continue;
            }
        }
        
        System.out.println("✅ Parsing terminé. " + hotspots.size() + " hotspots trouvés");
        return hotspots;
    }
}
