package be.mirooz.elitedangerous.lib.edtools;

import be.mirooz.elitedangerous.lib.edtools.client.EdToolsPveClient;
import be.mirooz.elitedangerous.lib.edtools.model.EdtoolResponse;

public class EdToolsScraper {
    public static void main(String[] args) throws Exception {
        EdToolsPveClient client = new EdToolsPveClient();
        EdtoolResponse result = client.fetch("Sol", 152, 5);

        java.lang.System.out.println("Rows: " + result.getRows().size());
        result.getRows().forEach(java.lang.System.out::println);
    }
}
