package be.mirooz.ardentapi.client;

import be.mirooz.ardentapi.model.CommodityMaxSell;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
    private HttpResponse<String>  fetchHtml(String url) throws IOException {
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
}
