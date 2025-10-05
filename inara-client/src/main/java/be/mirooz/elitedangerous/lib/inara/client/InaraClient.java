package be.mirooz.elitedangerous.lib.inara.client;

import be.mirooz.elitedangerous.lib.inara.model.NearbyStation;
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
            "https://inara.cz/elite/nearest-stations/?formbrief=1&pi17=1&pa1%5B%5D=8&pi25=1&pa2%5B%5D=3&pa2%5B%5D=2&pi26=1&ps1=";

    public List<NearbyStation> fetchNearbyStations(String systemName) throws IOException {
        String encodedSystem = URLEncoder.encode(systemName, StandardCharsets.UTF_8);
        String url = BASE_URL + encodedSystem;

        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .get();

        List<NearbyStation> stations = new ArrayList<>();
        Elements rows = doc.select("table.tablesortercollapsed tbody tr");

        for (Element row : rows) {
            Elements cells = row.select("td");
            if (cells.size() >= 8) {
                String stationName = cells.get(0).text();

                // Nettoyer systemName et distance
                String rawSystemName = cells.get(1).text();
                String systemNameClean = rawSystemName.replaceAll("[^\\p{L}\\p{N} \\-']", "").trim();

                String economy = cells.get(2).text();
                String government = cells.get(3).text();
                String allegiance = cells.get(4).text();

                String rawDistance = cells.get(6).text();
                String distanceLy = rawDistance.replaceAll("[^0-9.,Ly ]", "").trim();

                String updated = cells.get(7).text().trim();

                NearbyStation station = NearbyStation.builder()
                        .stationName(stationName)
                        .systemName(systemNameClean)
                        .distanceLy(distanceLy)
                        .updated(updated)
                        .economy(economy)
                        .government(government)
                        .allegiance(allegiance)
                        .build();

                stations.add(station);
            }
        }

        return stations;
    }
}
