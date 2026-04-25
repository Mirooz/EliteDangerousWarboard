package be.mirooz.elitedangerous.dashboard.model.registries;

import be.mirooz.elitedangerous.commons.lib.models.commodities.CommodityCategory;
import be.mirooz.elitedangerous.commons.lib.models.commodities.CommodityLoader;
import be.mirooz.elitedangerous.commons.lib.models.commodities.ICommodity;
import be.mirooz.elitedangerous.commons.lib.models.commodities.ICommodityFactory;
import be.mirooz.elitedangerous.commons.lib.models.commodities.RegistryCommodity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Catalogue global de commodités, singleton comme les autres {@code *Registry} du module.
 * <p>
 * <strong>Persistence</strong> : enregistré dans
 * {@link be.mirooz.elitedangerous.dashboard.service.persistence.PersistenceService} via
 * {@link be.mirooz.elitedangerous.dashboard.persistence.DashboardRegistryJsonPersistence#buildRegistryStores}
 * (fichier {@code commodity-registry.json}). Doit rester
 * <em>en tête de liste</em> de chargement : les restitutions {@code carrier-status},
 * {@code commander-ship}, etc. appellent {@link #resolve(String, String)} et supposent le
 * catalogue prêt dès
 * {@link be.mirooz.elitedangerous.dashboard.service.persistence.PersistenceService#loadAll()}.
 * <p>
 * Sans fichier (ou snapshot vide), le catalogue est replié sur {@link CommodityLoader}
 * (même contenu que le JSON classpath).
 */
public final class CommodityRegistry {

    private static final CommodityRegistry INSTANCE = new CommodityRegistry();

    @JsonIgnore
    private final List<ICommodity> commodities = new ArrayList<>();

    private CommodityRegistry() {}

    public static CommodityRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Résout une commodité d’abord sur ce catalogue persisté, puis repli
     * {@link ICommodityFactory#fromPersisted} (minéraux, limpets, commodités inconnues).
     */
    public synchronized ICommodity resolve(String cargoJsonName, String inaraName) {
        if (cargoJsonName != null && !cargoJsonName.isBlank()) {
            String k = normalizeCargo(cargoJsonName);
            for (ICommodity c : commodities) {
                if (c == null || c.getCargoJsonName() == null) {
                    continue;
                }
                if (normalizeCargo(c.getCargoJsonName()).equals(k)) {
                    return c;
                }
            }
        }
        if (inaraName != null && !inaraName.isBlank()) {
            String t = inaraName.trim();
            if (!t.isEmpty() && t.chars().allMatch(ch -> ch >= '0' && ch <= '9')) {
                for (ICommodity c : commodities) {
                    if (c == null) {
                        continue;
                    }
                    if (t.equals(c.getInaraId())) {
                        return c;
                    }
                }
            }
        }
        return ICommodityFactory.fromPersisted(
                cargoJsonName != null ? cargoJsonName : "",
                inaraName != null ? inaraName : "");
    }

    public synchronized void applyFullSnapshot(List<Line> lines) {
        commodities.clear();
        if (lines == null) {
            return;
        }
        for (Line line : lines) {
            if (line == null || line.getCargoJsonName() == null || line.getCargoJsonName().isBlank()) {
                continue;
            }
            RegistryCommodity c = line.toRegistryCommodity();
            String key = normalizeCargo(c.getCargoJsonName());
            boolean exists = false;
            for (ICommodity existing : commodities) {
                if (existing != null && key.equals(normalizeCargo(existing.getCargoJsonName()))) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                commodities.add(c);
            }
        }
    }

    /**
     * Si le catalogue est vide (pas encore restauré), remplit à partir de {@link CommodityLoader}.
     */
    public synchronized void ensureSeededFromClasspathIfEmpty() {
        if (!commodities.isEmpty()) {
            return;
        }
        for (RegistryCommodity c : CommodityLoader.allRegistryCommodities()) {
            commodities.add(c);
        }
    }

    public synchronized List<Line> exportLines() {
        List<Line> out = new ArrayList<>(commodities.size());
        for (ICommodity c : commodities) {
            out.add(Line.from(c));
        }
        return out;
    }

    @JsonProperty("commodities")
    public synchronized List<Line> getPersistedCommodities() {
        ensureSeededFromClasspathIfEmpty();
        return exportLines();
    }

    @JsonProperty("commodities")
    public synchronized void setPersistedCommodities(List<Line> lines) {
        if (lines == null || lines.isEmpty()) {
            applyFullSnapshot(List.of());
            ensureSeededFromClasspathIfEmpty();
            return;
        }
        applyFullSnapshot(lines);
    }

    private static String normalizeCargo(String s) {
        if (s == null) {
            return "";
        }
        return s.toLowerCase(Locale.ROOT).trim();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Line {
        private String cargoJsonName;
        private String inaraId;
        private String inaraName;
        private String inaraCategory;

        public static Line from(ICommodity c) {
            String cat = c.getInaraCommodityCategory() != null
                    ? c.getInaraCommodityCategory().name()
                    : CommodityCategory.UNKNOWN.name();
            return new Line(
                    c.getCargoJsonName(),
                    c.getInaraId(),
                    c.getInaraName(),
                    cat
            );
        }

        public RegistryCommodity toRegistryCommodity() {
            String iname = inaraName != null && !inaraName.isBlank() ? inaraName : cargoJsonName;
            return new RegistryCommodity(
                    cargoJsonName,
                    inaraId != null && !inaraId.isBlank() ? inaraId : null,
                    iname,
                    CommodityCategory.fromRegistryValue(inaraCategory)
            );
        }
    }

}
