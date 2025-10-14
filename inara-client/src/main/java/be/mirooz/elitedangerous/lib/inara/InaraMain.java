package be.mirooz.elitedangerous.lib.inara;

import be.mirooz.elitedangerous.lib.inara.client.InaraClient;
import be.mirooz.elitedangerous.lib.inara.model.commodities.minerals.CoreMineralType;
import java.io.IOException;


public class InaraMain {
    public static void main(String[] args) throws IOException {

        InaraClient client = new InaraClient();
        System.out.println(client.fetchConflictSystems("LP 908-11"));
        System.out.println(client.fetchMinerMarket(CoreMineralType.fromCargoJsonName("benitoite").get(),"Sol",100,500,false));
    }
}
