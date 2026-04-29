package be.mirooz.elitedangerous.eddn;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Retire avant publication EDDN les données incompatibles avec les schémas officiels bundlés.
 *
 * <p><b>Racine du message</b> : propriétés listées comme {@code $ref → #/definitions/disallowed} dans
 * {@code properties.message.properties} du schéma correspondant au {@code schemaRef} — voir
 * {@link EddnSchemaPublishStripIndex} (aucune liste de champs en dur).</p>
 *
 * <p><b>Récursion</b> : mêmes schémas déterminent les noms de propriétés {@code disallowed} en profondeur
 * (hors premier niveau de {@code message.properties}) ; elles sont retirées sur tout objet du JSON.</p>
 *
 * <p><b>Motifs</b> : les clés de {@code patternProperties} sont des <b>regex</b> (dialecte JSON Schema /
 * ECMA-262 dans les schémas EDDN). On ne garde que celles dont la valeur est {@code $ref → disallowed}
 * (ex. {@code _Localised$}) ; une entrée comme {@code ^(Materials|StationEconomies|Signals)$} avec un
 * schéma de tableau n’est pas une règle de retrait — le parcours descend seulement dedans pour trouver
 * d’éventuels motifs {@code disallowed} imbriqués (ex. {@code _Localised$} sous {@code items}).</p>
 */
public final class EddnPersonalDataStripper {

    private EddnPersonalDataStripper() {}

    /**
     * @param schemaRef URI du schéma (ex. {@link EddnSchemas#JOURNAL_V1}) ; si {@code null} ou inconnu,
     *                  seuls le nettoyage récursif (noms {@code disallowed} imbriqués + motifs
     *                  {@code patternProperties} / {@code disallowed}) s’appliquent, pas de retraits
     *                  spécifiques à la racine du message.
     */
    public static void stripInPlace(ObjectNode node, String schemaRef) {
        if (node == null) {
            return;
        }
        for (String field : EddnSchemaPublishStripIndex.rootDisallowedKeysForSchemaRef(schemaRef)) {
            node.remove(field);
        }
        stripNestedRecursive(
                node,
                EddnSchemaPublishStripIndex.nestedDisallowedPropertyNames(),
                EddnSchemaPublishStripIndex.nestedDisallowedPropertyNamePatterns());
    }

    private static void stripNestedRecursive(JsonNode node,
                                             Set<String> nestedDisallowedNames,
                                             List<Pattern> nestedDisallowedPatterns) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            for (String f : nestedDisallowedNames) {
                obj.remove(f);
            }
            for (String k : collectKeysMatchingPatterns(obj, nestedDisallowedPatterns)) {
                obj.remove(k);
            }
            for (Iterator<JsonNode> it = obj.elements(); it.hasNext(); ) {
                stripNestedRecursive(it.next(), nestedDisallowedNames, nestedDisallowedPatterns);
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                stripNestedRecursive(child, nestedDisallowedNames, nestedDisallowedPatterns);
            }
        }
    }

    private static List<String> collectKeysMatchingPatterns(ObjectNode obj, List<Pattern> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return List.of();
        }
        List<String> keys = new ArrayList<>();
        for (Iterator<Map.Entry<String, JsonNode>> it = obj.fields(); it.hasNext(); ) {
            String name = it.next().getKey();
            for (Pattern p : patterns) {
                // JSON Schema : la clé de patternProperties est une regex testée sur le nom de propriété
                // (équivalent ECMA RegExp#test), pas Matcher#matches() Java (ancrage implicite sur toute la chaîne).
                if (p.matcher(name).find()) {
                    keys.add(name);
                    break;
                }
            }
        }
        return keys;
    }
}
