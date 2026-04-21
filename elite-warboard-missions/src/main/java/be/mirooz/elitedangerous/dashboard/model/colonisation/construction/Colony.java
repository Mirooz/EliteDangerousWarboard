package be.mirooz.elitedangerous.dashboard.model.colonisation.construction;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Registre colonie / construction : charge {@code /json/construction_class.json} au premier
 * {@link #getInstance()}. Fournit aussi un petit modèle de score pour des structures « posées »
 * sur le palier courant {@link #tier}, et des utilitaires statiques (clé de persistance, listes UI,
 * texte de synthèse) pour les entrées {@link Structure}.
 */
public final class Colony {

    /** Séparateur improbable dans les champs JSON (catégorie / type / nom), pour la persistance préférences. */
    private static final char STRUCTURE_KEY_SEP = '\u001f';

    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE);

    private static final TypeReference<List<Structure>> LIST_TYPE = new TypeReference<>() {};

    private static final class Holder {
        private static final Colony INSTANCE = new Colony();
    }

    /** Définitions du JSON (immuable). */
    private final List<Structure> constructionClassCatalog;

    private int tier = 2;
    private final List<Structure> structures = new ArrayList<>();

    private Colony() {
        this.constructionClassCatalog = List.copyOf(loadConstructionClassesFromClasspath());
    }

    /**
     * Instance unique ; au premier appel, lit et parse {@code construction_class.json}.
     */
    public static Colony getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Toutes les lignes de {@code construction_class.json} (catalogue référence).
     */
    public List<Structure> getConstructionClassCatalog() {
        return constructionClassCatalog;
    }

    public int getTier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = tier;
    }

    public void addStructure(Structure s) {
        structures.add(s);
    }

    public int calculateScore() {
        return structures.stream()
                .mapToInt(s -> s.getValueForTier(tier))
                .sum();
    }

    /** Clé stable pour {@code ~/.elite-warboard/colonisation-construction-structure-types.properties}. */
    public static String persistedStructureKey(Structure s) {
        Objects.requireNonNull(s, "structure");
        return s.category + STRUCTURE_KEY_SEP + s.type + STRUCTURE_KEY_SEP + s.name;
    }

    public static Optional<Structure> structureFromPersistedKey(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            return Optional.empty();
        }
        String key = rawKey.strip();
        String[] p = key.split(Pattern.quote(String.valueOf(STRUCTURE_KEY_SEP)), -1);
        if (p.length != 3) {
            return Optional.empty();
        }
        return getInstance().getConstructionClassCatalog().stream()
                .filter(s -> p[0].equals(s.category) && p[1].equals(s.type) && p[2].equals(s.name))
                .findFirst();
    }

    public static List<String> distinctCategories(List<Structure> catalog) {
        Set<String> set = new LinkedHashSet<>();
        for (Structure s : catalog) {
            if (s.category != null && !s.category.isBlank()) {
                set.add(s.category);
            }
        }
        List<String> out = new ArrayList<>(set);
        out.sort(String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    public static List<String> distinctTypes(List<Structure> catalog, String category) {
        if (category == null || category.isBlank()) {
            return List.of();
        }
        Set<String> set = new LinkedHashSet<>();
        for (Structure s : catalog) {
            if (category.equals(s.category) && s.type != null && !s.type.isBlank()) {
                set.add(s.type);
            }
        }
        List<String> out = new ArrayList<>(set);
        out.sort(String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    public static List<Structure> structuresFor(List<Structure> catalog, String category, String type) {
        if (category == null || category.isBlank() || type == null || type.isBlank()) {
            return List.of();
        }
        return catalog.stream()
                .filter(s -> category.equals(s.category) && type.equals(s.type))
                .sorted(Comparator.comparing(s -> s.name != null ? s.name : "", String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** Infobulle : données JSON (pas de tonnages cargo). */
    public static String structureSummaryText(Structure s) {
        if (s == null) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        b.append(s.category).append(" — ").append(s.type).append(" — ").append(s.name);
        if (s.cost != null && !s.cost.isEmpty()) {
            b.append('\n').append("Cost: ");
            s.cost.entrySet().stream()
                    .sorted(Comparator.comparing(e -> e.getKey().toLowerCase(Locale.ROOT)))
                    .forEach(e -> b.append(e.getKey()).append('=').append(e.getValue()).append(' '));
        }
        if (s.earning != null && !s.earning.isEmpty()) {
            b.append('\n').append("Earning: ");
            s.earning.entrySet().stream()
                    .sorted(Comparator.comparing(e -> e.getKey().toLowerCase(Locale.ROOT)))
                    .forEach(e -> b.append(e.getKey()).append('=').append(e.getValue()).append(' '));
        }
        if (s.stats != null) {
            appendStatIf(b, "Security", s.stats.security);
            appendStatIf(b, "Tech", s.stats.techLevel);
            appendStatIf(b, "Wealth", s.stats.wealth);
            appendStatIf(b, "Standard of living", s.stats.standardOfLiving);
            appendStatIf(b, "Development", s.stats.developmentLevel);
        }
        if (s.economy != null && s.economy.type != null && !s.economy.type.isBlank()) {
            b.append('\n').append("Economy: ").append(s.economy.type);
        }
        if (s.population != null) {
            appendStatIf(b, "Pop. initial +", s.population.initialIncrease);
            appendStatIf(b, "Pop. max +", s.population.maxIncrease);
        }
        return b.toString().strip();
    }

    private static void appendStatIf(StringBuilder b, String label, Integer v) {
        if (v != null) {
            b.append('\n').append(label).append(" : ").append(v);
        }
    }

    private static List<Structure> loadConstructionClassesFromClasspath() {
        try (InputStream in = Colony.class.getResourceAsStream("/json/construction_class.json")) {
            if (in == null) {
                throw new IOException("Ressource introuvable: /json/construction_class.json");
            }
            return JSON.readValue(in, LIST_TYPE);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
