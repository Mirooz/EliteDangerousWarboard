// src/main/java/be/mirooz/elitedangerous/edtools/client/EdToolsPveClient.java
package be.mirooz.elitedangerous.lib.edtools.client;

import be.mirooz.elitedangerous.lib.edtools.model.EdtoolResponse;
import be.mirooz.elitedangerous.lib.edtools.model.MassacreSystem;
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

public class EdToolsPveClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public EdtoolResponse fetch(String referenceSystem, int maxDistanceLy, int minSourcesPerTarget) throws IllegalArgumentException, IOException, InterruptedException {
        validateParams(maxDistanceLy, minSourcesPerTarget);
        String s = URLEncoder.encode(referenceSystem, StandardCharsets.UTF_8);
        String url = String.format("https://edtools.cc/pve?s=%s&md=%d&sc=%d", s, maxDistanceLy, minSourcesPerTarget);

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
}
