package be.mirooz.ardentapi;

import be.mirooz.ardentapi.client.ArdentApiClient;
import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.MineralType;

import java.io.IOException;

public class ArdentApiMain {
    public static void main(String[] args) throws IOException {

        ArdentApiClient ardentApiClient = new ArdentApiClient();
        System.out.println(ardentApiClient.fetchCommoditiesMaxSell());
        System.out.println(ardentApiClient.fetchMinerMarket(MineralType.fromCargoJsonName("benitoite").get(),
                "Luyten's Star",
                100,
                500,
                true,
                true));
        System.out.println(ardentApiClient.fetchStationMarket("128106744"));
    }
}