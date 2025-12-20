package be.mirooz.elitedangerous;

import be.mirooz.elitedangerous.client.ArdentApiClient;

import java.io.IOException;

public class ArdentApiMain {
    public static void main(String[] args) throws IOException {

        ArdentApiClient ardentApiClient = new ArdentApiClient();
        System.out.println(ardentApiClient.fetchCommoditiesMaxSell());
    }
}