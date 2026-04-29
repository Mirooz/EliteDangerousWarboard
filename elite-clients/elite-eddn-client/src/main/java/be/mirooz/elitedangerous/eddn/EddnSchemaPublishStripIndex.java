package be.mirooz.elitedangerous.eddn;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Index des propriétés à retirer avant publication EDDN, dérivé des schémas JSON officiels bundlés
 * ({@code /eddn-schemas/*.json}) : aucune liste de champs maintenue à la main.
 *
 * <p><b>Racine du {@code message}</b> : pour chaque schéma, les clés de {@code properties.message.properties}
 * dont la définition est {@code "$ref": "#/definitions/disallowed"} — indexées par l’URI du schéma
 * ({@code id} normalisé, aligné sur {@link EddnSchemas}).</p>
 *
 * <p><b>Imbrication</b> : toute propriété (à n’importe quelle profondeur dans le schéma) déclarée comme
 * {@code $ref → disallowed} sous un bloc {@code properties} <i>autre que</i> le premier niveau
 * {@code message.properties} (ex. entrées de {@code Factions}, signaux FSS…) est retirée récursivement
 * sur l’instance JSON.</p>
 *
 * <p><b>{@code patternProperties}</b> : chaque <b>clé</b> est une chaîne regex (ECMA-262 côté schéma).
 * Seules les entrées dont la valeur est exactement {@code "$ref": "#/definitions/disallowed"} sont des
 * règles de strip (ex. {@code "_Localised$"}). Les autres motifs (ex.
 * {@code ^(Materials|StationEconomies|Signals)$} avec un schéma de tableau) ne retirent rien par eux-mêmes
 * mais sont parcourus pour collecter les {@code disallowed} plus bas (ex. {@code _Localised$} dans
 * {@code items.patternProperties}).</p>
 *
 * <p>À l’application, chaque nom de propriété JSON est testé comme en validateur JSON Schema habituel
 * (proche de {@code RegExp(motif).test(nom)}), via {@link java.util.regex.Matcher#find()} — pas
 * {@link java.util.regex.Matcher#matches()}.</p>
 */
public final class EddnSchemaPublishStripIndex {

    private static final String DISALLOWED_REF = "#/definitions/disallowed";

    /** Fichiers schéma versionnés dans le module (à tenir à jour si EDDN ajoute un schéma). */
    private static final String[] SCHEMA_FILES = {
            "approachsettlement.json",
            "codexentry.json",
            "commodity.json",
            "dockingdenied.json",
            "dockinggranted.json",
            "fcmaterials_capi.json",
            "fcmaterials_journal.json",
            "fssallbodiesfound.json",
            "fssbodysignals.json",
            "fssdiscoveryscan.json",
            "fsssignaldiscovered.json",
            "journal.json",
            "navbeaconscan.json",
            "navroute.json",
            "outfitting.json",
            "scanbarycentre.json",
            "shipyard.json"
    };

    private static final Map<String, Set<String>> ROOT_DISALLOWED_BY_SCHEMA_URI;
    private static final Set<String> NESTED_DISALLOWED_PROPERTY_NAMES;
    private static final List<Pattern> NESTED_DISALLOWED_PROPERTY_NAME_PATTERNS;

    static {
        try {
            ObjectMapper mapper = EddnEnvelope.mapper();
            Map<String, Set<String>> roots = new HashMap<>();
            LinkedHashSet<String> nested = new LinkedHashSet<>();
            LinkedHashSet<String> nestedPatternStrings = new LinkedHashSet<>();
            for (String file : SCHEMA_FILES) {
                String resource = "/eddn-schemas/" + file;
                try (InputStream in = EddnSchemaPublishStripIndex.class.getResourceAsStream(resource)) {
                    if (in == null) {
                        throw new IllegalStateException("Schéma EDDN introuvable sur le classpath : " + resource);
                    }
                    JsonNode root = mapper.readTree(in);
                    String id = normalizeSchemaUri(root.path("id").asText(""));
                    if (id.isEmpty()) {
                        continue;
                    }
                    JsonNode messageProps = root.path("properties").path("message").path("properties");
                    if (!messageProps.isObject()) {
                        continue;
                    }
                    LinkedHashSet<String> rootKeys = new LinkedHashSet<>();
                    messageProps.fields().forEachRemaining(e -> {
                        if (isDirectDisallowedRef(e.getValue())) {
                            rootKeys.add(e.getKey());
                        }
                    });
                    if (!rootKeys.isEmpty()) {
                        roots.put(id, Collections.unmodifiableSet(rootKeys));
                    }
                    collectNestedDisallowedNames(messageProps, nested, nestedPatternStrings);
                }
            }
            Map<String, Set<String>> frozenRoots = new HashMap<>();
            roots.forEach((k, v) -> frozenRoots.put(k, v));
            ROOT_DISALLOWED_BY_SCHEMA_URI = Collections.unmodifiableMap(frozenRoots);
            NESTED_DISALLOWED_PROPERTY_NAMES = Collections.unmodifiableSet(nested);
            NESTED_DISALLOWED_PROPERTY_NAME_PATTERNS = compileNestedPatterns(nestedPatternStrings);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private EddnSchemaPublishStripIndex() {}

    static String normalizeSchemaUri(String rawId) {
        if (rawId == null) {
            return "";
        }
        String s = rawId.trim();
        int h = s.indexOf('#');
        if (h >= 0) {
            s = s.substring(0, h);
        }
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    /**
     * Clés à retirer à la racine du corps {@code message} pour ce {@code schemaRef} (constante
     * {@link EddnSchemas}), ou ensemble vide si inconnu.
     */
    public static Set<String> rootDisallowedKeysForSchemaRef(String schemaRef) {
        if (schemaRef == null || schemaRef.isBlank()) {
            return Set.of();
        }
        String key = normalizeSchemaUri(schemaRef);
        return ROOT_DISALLOWED_BY_SCHEMA_URI.getOrDefault(key, Set.of());
    }

    /** Noms de propriétés à retirer sur tout objet du JSON message (hors racine déjà traitée par schéma). */
    public static Set<String> nestedDisallowedPropertyNames() {
        return NESTED_DISALLOWED_PROPERTY_NAMES;
    }

    /**
     * Regex (clés de {@code patternProperties}) dont la valeur est {@code $ref → disallowed} ; appliquées
     * aux noms de champs avec {@link Pattern#matcher(CharSequence)} puis {@link java.util.regex.Matcher#find()}
     * (aligné sur l’usage type {@code RegExp#test} pour les noms de propriétés, pas {@code matches()} Java).
     */
    public static List<Pattern> nestedDisallowedPropertyNamePatterns() {
        return NESTED_DISALLOWED_PROPERTY_NAME_PATTERNS;
    }

    private static List<Pattern> compileNestedPatterns(LinkedHashSet<String> patternStrings) {
        List<Pattern> out = new ArrayList<>(patternStrings.size());
        for (String s : patternStrings) {
            try {
                out.add(Pattern.compile(s));
            } catch (PatternSyntaxException e) {
                throw new ExceptionInInitializerError(new IllegalStateException(
                        "Motif patternProperties EDDN invalide pour publication : " + s, e));
            }
        }
        return List.copyOf(out);
    }

    private static boolean isDirectDisallowedRef(JsonNode def) {
        return def != null && def.isObject() && DISALLOWED_REF.equals(def.path("$ref").asText());
    }

    /**
     * Parcourt les valeurs sous {@code message.properties} (sans ajouter les clés de ce niveau) et
     * collecte les noms de propriétés {@code disallowed} rencontrés plus bas dans l’arbre du schéma.
     */
    private static void collectNestedDisallowedNames(JsonNode messagePropertiesNode,
                                                     Set<String> nameSink,
                                                     Set<String> patternStringSink) {
        messagePropertiesNode.fields().forEachRemaining(e ->
                walkSchemaForNestedDisallowed(e.getValue(), nameSink, patternStringSink));
    }

    private static void walkSchemaForNestedDisallowed(JsonNode n,
                                                      Set<String> nameSink,
                                                      Set<String> patternStringSink) {
        if (n == null) {
            return;
        }
        if (n.isObject()) {
            JsonNode props = n.get("properties");
            if (props != null && props.isObject()) {
                props.fields().forEachRemaining(e -> {
                    JsonNode def = e.getValue();
                    if (isDirectDisallowedRef(def)) {
                        nameSink.add(e.getKey());
                    }
                    walkSchemaForNestedDisallowed(def, nameSink, patternStringSink);
                });
            }
            JsonNode patternProps = n.get("patternProperties");
            if (patternProps != null && patternProps.isObject()) {
                patternProps.fields().forEachRemaining(e -> {
                    JsonNode def = e.getValue();
                    if (isDirectDisallowedRef(def)) {
                        patternStringSink.add(e.getKey());
                    }
                    walkSchemaForNestedDisallowed(def, nameSink, patternStringSink);
                });
            }
            n.fields().forEachRemaining(e -> {
                if (!"properties".equals(e.getKey()) && !"patternProperties".equals(e.getKey())) {
                    walkSchemaForNestedDisallowed(e.getValue(), nameSink, patternStringSink);
                }
            });
        } else if (n.isArray()) {
            for (JsonNode c : n) {
                walkSchemaForNestedDisallowed(c, nameSink, patternStringSink);
            }
        }
    }
}
