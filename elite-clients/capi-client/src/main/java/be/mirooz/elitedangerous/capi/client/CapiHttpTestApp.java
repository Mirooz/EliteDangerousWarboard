package be.mirooz.elitedangerous.capi.client;

import be.mirooz.elitedangerous.capi.client.relay.CapiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Instant;

/**
 * App console : simulation d'un event journal {@code Market} et appel de
 * {@code CapiApiService.sendMarketDatas(event)}.
 * <p>
 * Lancement recommandé (classpath contenant warboard) :
 * {@code mvn -pl elite-warboard-missions exec:java -Dexec.mainClass=be.mirooz.elitedangerous.capi.client.CapiHttpTestApp}
 * <br>
 * Profil : ajouter {@code -Dapp.profile=prod}
 */
public final class CapiHttpTestApp {

    public static void main(String[] args) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode root = mapper.createObjectNode();
        root.put("fid", "F2290564");
        root.put("commanderName", "Mirooz");

        ObjectNode event = mapper.createObjectNode();
        event.put("timestamp", "2026-04-14T12:21:28Z");
        event.put("event", "Market");
        event.put("MarketID", 3714426624L);
        event.put("StationName", "G2W-8HB");
        event.put("StationType", "FleetCarrier");
        event.put("CarrierDockingAccess", "all");
        event.put("StarSystem", "Cemiess");

        root.set("event", event);
        JsonNode response = CapiClient.getInstance().sendMarket(root);
        System.out.println(response);
    }
}
