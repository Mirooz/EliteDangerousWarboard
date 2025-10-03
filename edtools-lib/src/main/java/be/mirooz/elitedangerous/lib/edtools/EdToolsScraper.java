package be.mirooz.elitedangerous.lib.edtools;

import be.mirooz.elitedangerous.lib.edtools.client.EdToolsPveClient;
import be.mirooz.elitedangerous.lib.edtools.model.EdtoolResponse;

public class EdToolsScraper {
    public static void main(String[] args) throws Exception {
        EdToolsPveClient client = new EdToolsPveClient();
        EdtoolResponse result = client.sendSystemSearch("Sol", 152, 1,true);

        java.lang.System.out.println("Rows: " + result.getRows().size());
        result.getRows().forEach(java.lang.System.out::println);

        EdtoolResponse result2 = client.sendTargetSystemSearch("Core Sys Sector OI-T b3-6");

        java.lang.System.out.println("Rows: " + result2.getRows().size());
        result2.getRows().forEach(java.lang.System.out::println);
    }
}
