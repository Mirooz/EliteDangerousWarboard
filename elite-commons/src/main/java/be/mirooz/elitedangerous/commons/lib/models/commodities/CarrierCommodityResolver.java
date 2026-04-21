package be.mirooz.elitedangerous.commons.lib.models.commodities;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Associe les chaînes journal / CAPI ({@code Commodity}, {@code Name}, libellés) à un {@link ICommodity}.
 */
public final class CarrierCommodityResolver {

    private static final ConcurrentHashMap<String, CarrierUnresolvedCommodity> UNRESOLVED =
            new ConcurrentHashMap<>();

    private CarrierCommodityResolver() {}

    public static ICommodity resolve(String internal, String localized) {
        if (internal != null && !internal.isBlank()) {
            Optional<ICommodity> o = ICommodityFactory.ofByCargoJson(internal);
            if (o.isPresent()) {
                return o.get();
            }
        }
        if (localized != null && !localized.isBlank()) {
            Optional<ICommodity> o = ICommodityFactory.ofByCargoJson(localized);
            if (o.isPresent()) {
                return o.get();
            }
        }
        String key =
                internal != null && !internal.isBlank()
                        ? internal.trim().toLowerCase(Locale.ROOT)
                        : (localized != null && !localized.isBlank()
                                ? localized.trim().toLowerCase(Locale.ROOT)
                                : "__unknown_commodity__");
        String display = firstNonBlank(localized, internal, key);
        return UNRESOLVED.computeIfAbsent(key, k -> new CarrierUnresolvedCommodity(k, display));
    }

    private static String firstNonBlank(String... parts) {
        if (parts == null) {
            return "";
        }
        for (String p : parts) {
            if (p != null && !p.isBlank()) {
                return p;
            }
        }
        return "";
    }
}
