package be.mirooz.elitedangerous.eddn;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Retire les champs classés "personnels" / "non-EDDN" avant publication.
 *
 * <p>Aligné sur les règles EDMC (client de référence EDDN) :</p>
 * <ul>
 *   <li>Champs personnels supprimés à la racine (et certains aussi récursivement).</li>
 *   <li>Tous les champs {@code *_Localised} supprimés récursivement : ce sont des traductions
 *       côté client, non canoniques ; plusieurs schémas EDDN les refusent explicitement
 *       ({@code additionalProperties: false}).</li>
 * </ul>
 */
public final class EddnPersonalDataStripper {

    private static final String LOCALISED_SUFFIX = "_Localised";

    /**
     * Champs à retirer à la racine du message EDDN.
     *
     * <p>Aligné sur EDMC : on ne retire QUE les champs clairement personnels (carburant,
     * boost, crime, réputation). {@code Latitude}/{@code Longitude} sont conservés car pour
     * certains events comme {@code ApproachSettlement} ils désignent la position du bâtiment
     * sur le corps (donnée non-personnelle requise par le schéma).
     */
    private static final List<String> TOP_LEVEL_BLACKLIST = Arrays.asList(
            "ActiveFine",
            "BoostUsed",
            "CockpitBreach",
            "FuelLevel",
            "FuelUsed",
            "JumpDist",
            "MyReputation",
            "SquadronFaction",
            "HappiestSystem",
            "HomeSystem",
            "Wanted",
            "CommanderName",
            // Schémas codexentry/1 et journal/1 : champs journal présents mais interdits à l’envoi (PII).
            "IsNewEntry",
            "NewTraitsDiscovered",
            // fssdiscoveryscan/1 : interdit si présent (le mapper le retire déjà sur l’ObjectNode).
            "Progress"
    );

    /** Champs à retirer récursivement dans toutes les sous-entités (factions, stations, etc.). */
    private static final List<String> NESTED_BLACKLIST = Arrays.asList(
            "MyReputation",
            "SquadronFaction",
            "HappiestSystem",
            "HomeSystem",
            // fsssignaldiscovered/1 : dans chaque entrée de signals[] (PII / éphémère).
            "TimeRemaining"
    );

    private EddnPersonalDataStripper() {}

    public static void stripInPlace(ObjectNode node) {
        if (node == null) {
            return;
        }
        for (String field : TOP_LEVEL_BLACKLIST) {
            node.remove(field);
        }
        stripNestedRecursive(node);
    }

    private static void stripNestedRecursive(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            for (String f : NESTED_BLACKLIST) {
                obj.remove(f);
            }
            List<String> toRemove = collectLocalisedKeys(obj);
            for (String k : toRemove) {
                obj.remove(k);
            }
            for (Iterator<JsonNode> it = obj.elements(); it.hasNext(); ) {
                stripNestedRecursive(it.next());
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                stripNestedRecursive(child);
            }
        }
    }

    private static List<String> collectLocalisedKeys(ObjectNode obj) {
        List<String> keys = new ArrayList<>();
        for (Iterator<Map.Entry<String, JsonNode>> it = obj.fields(); it.hasNext(); ) {
            String name = it.next().getKey();
            if (name.endsWith(LOCALISED_SUFFIX)) {
                keys.add(name);
            }
        }
        return keys;
    }
}
