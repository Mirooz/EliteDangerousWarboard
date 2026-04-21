package be.mirooz.elitedangerous.commons.lib.models.commodities;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Charge {@code /commodities/ardent-inara-registry.json} (classpath) une seule fois.
 */
public final class CommodityLoader {

    private static final String RESOURCE = "/commodities/ardent-inara-registry.json";

    private static volatile Map<String, RegistryCommodity> byCargoJson = Map.of();
    private static volatile Map<String, RegistryCommodity> byInaraId = Map.of();
    private static volatile boolean loadAttempted;

    private CommodityLoader() {}

    public static Optional<ICommodity> findByCargoJsonName(String cargoJsonName) {
        ensureLoaded();
        if (cargoJsonName == null || cargoJsonName.isBlank()) {
            return Optional.empty();
        }
        cargoJsonName = normalizeCargoName(cargoJsonName);
        String key = cargoJsonName.toLowerCase().trim();
        return Optional.ofNullable(byCargoJson.get(key)).map(ICommodity.class::cast);
    }

    public static Optional<ICommodity> findByInaraId(String inaraId) {
        ensureLoaded();
        if (inaraId == null || inaraId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(byInaraId.get(inaraId.trim())).map(ICommodity.class::cast);
    }
    private static String normalizeCargoName(String name) {
        if (name == null || name.isEmpty() || !name.contains("$")) {
            return name;
        }

        String cleaned = name.replace("$", "");
        int i = cleaned.indexOf('_');

        return i == -1 ? cleaned : cleaned.substring(0, i);
    }
    private static void ensureLoaded() {
        if (loadAttempted) {
            return;
        }
        synchronized (CommodityLoader.class) {
            if (loadAttempted) {
                return;
            }
            loadAttempted = true;
            try (InputStream in = CommodityLoader.class.getResourceAsStream(RESOURCE)) {
                if (in == null) {
                    System.err.println("CommodityLoader: ressource absente " + RESOURCE);
                    byCargoJson = Map.of();
                    byInaraId = Map.of();
                    return;
                }
                ObjectMapper mapper = new ObjectMapper();
                List<Map<String, Object>> rows =
                        mapper.readValue(in, new TypeReference<List<Map<String, Object>>>() {});
                Map<String, RegistryCommodity> cargo = new HashMap<>();
                Map<String, RegistryCommodity> inara = new HashMap<>();
                for (Map<String, Object> row : rows) {
                    String cargoName = stringVal(row.get("cargoJsonName"));
                    if (cargoName.isBlank()) {
                        continue;
                    }
                    String iid = stringVal(row.get("inaraId"));
                    if (iid.isEmpty()) {
                        iid = null;
                    }
                    String iname = stringVal(row.get("inaraName"));
                    if (iname.isEmpty()) {
                        iname = null;
                    }
                    CommodityCategory cat =
                            CommodityCategory.fromRegistryValue(stringVal(row.get("inaraCategory")));
                    RegistryCommodity c = new RegistryCommodity(cargoName, iid, iname, cat);
                    cargo.put(c.getCargoJsonName(), c);
                    if (iid != null && !inara.containsKey(iid)) {
                        inara.put(iid, c);
                    }
                }
                byCargoJson = Collections.unmodifiableMap(cargo);
                byInaraId = Collections.unmodifiableMap(inara);
            } catch (Exception e) {
                System.err.println("CommodityLoader: échec du chargement — " + e.getMessage());
                e.printStackTrace();
                byCargoJson = Map.of();
                byInaraId = Map.of();
            }
        }
    }

    private static String stringVal(Object o) {
        if (o == null) {
            return "";
        }
        String s = String.valueOf(o).trim();
        if ("null".equalsIgnoreCase(s)) {
            return "";
        }
        return s;
    }
}
