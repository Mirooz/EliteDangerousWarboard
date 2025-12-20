package be.mirooz.elitedangerous.siriuscorp.client;

import be.mirooz.elitedangerous.siriuscorp.model.ConflictSystem;
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

public class SiriuscorpClient {

    private static final String BASE_URL = "https://siriuscorp.cc";
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    public List<ConflictSystem> fetchConflictSystems(String sourceSystem) throws IOException {
        String encodedSystem = URLEncoder.encode(sourceSystem, StandardCharsets.UTF_8);
        String url = BASE_URL + "/conflict/?system=" + encodedSystem + "&radius=50";
        System.out.println("Fetching conflict list from: " + url);

        HttpResponse<String> response = fetchHtml(url);
        return parseHtml(response.body());
    }

    private List<ConflictSystem> parseHtml(String html) {
        List<ConflictSystem> systems = new ArrayList<>();

        Document doc = Jsoup.parse(html);
        Elements rows = doc.select("table tbody tr");

        for (Element row : rows) {
            Elements cols = row.select("td");
            if (cols.size() < 4) {
                continue;
            }

            ConflictSystem cs = new ConflictSystem();
            cs.setSystemName(
                    cols.get(0).selectFirst("span[id^=sys-]").text().trim()
            );

            cs.setDistanceLy(parseDouble(cols.get(1).text()));
            cs.setFaction(cols.get(2).text());
            cs.setOpponentFaction(cols.get(3).text());
            cs.setSurfaceConflicts(0); // demandé 👍

            systems.add(cs);
        }

        return systems;
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0.0;
        }
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
                response.headers().firstValue("Retry-After")
                        .ifPresent(ra -> System.out.println("Retry-After (sec): " + ra));
            }

            return response;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("HTTP request interrupted", e);
        }
    }
}
