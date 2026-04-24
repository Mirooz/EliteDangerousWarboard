package be.mirooz.elitedangerous.eddn;

import be.mirooz.elitedangerous.eddn.generated.EddnMessage;
import be.mirooz.elitedangerous.eddn.generated.EddnMessage__1;
import be.mirooz.elitedangerous.eddn.generated.EddnMessage__10;
import be.mirooz.elitedangerous.eddn.generated.EddnMessage__11;
import be.mirooz.elitedangerous.eddn.generated.EddnMessage__12;
import be.mirooz.elitedangerous.eddn.generated.EddnMessage__13;
import be.mirooz.elitedangerous.eddn.generated.EddnMessage__14;
import be.mirooz.elitedangerous.eddn.generated.EddnMessage__15;
import be.mirooz.elitedangerous.eddn.generated.EddnMessage__16;
import be.mirooz.elitedangerous.eddn.generated.EddnMessage__2;
import be.mirooz.elitedangerous.eddn.generated.EddnMessage__3;
import be.mirooz.elitedangerous.eddn.generated.EddnMessage__4;
import be.mirooz.elitedangerous.eddn.generated.EddnMessage__6;
import be.mirooz.elitedangerous.eddn.generated.EddnMessage__7;
import be.mirooz.elitedangerous.eddn.generated.EddnMessage__8;
import be.mirooz.elitedangerous.eddn.generated.EddnMessage__9;

/**
 * Façade stable sur les POJOs Jackson générés par {@code jsonschema2pojo} à partir des JSON Schema EDDN.
 *
 * <p>Les classes générées portent des noms techniques ({@code EddnMessage__N}) parce que chaque schéma
 * définit un type {@code message} homonyme et que le générateur doit les dédoublonner. Cette façade
 * re-exporte ces types sous des noms métier stables : c'est le <b>seul</b> endroit à mettre à jour si
 * l'ordre des fichiers sources change et que les numéros {@code __N} sont réassignés.</p>
 *
 * <p>L'extension simple (sans champ ni méthode) n'altère ni la sérialisation Jackson (hérite
 * {@code @JsonProperty}/{@code @JsonPropertyOrder} des classes mères) ni le contrat du schéma
 * ({@code additionalProperties: false} pour les schémas stricts).</p>
 *
 * <p>Mapping schéma → classe mère (ordre alphabétique des fichiers sous {@code eddn-schemas/},
 * tel qu'attribué par jsonschema2pojo) :</p>
 * <ul>
 *   <li>{@code approachsettlement.json}  → {@link EddnMessage}</li>
 *   <li>{@code codexentry.json}          → {@link EddnMessage__1}</li>
 *   <li>{@code commodity.json}           → {@link EddnMessage__2}</li>
 *   <li>{@code dockingdenied.json}       → {@link EddnMessage__3}</li>
 *   <li>{@code dockinggranted.json}      → {@link EddnMessage__4}</li>
 *   <li>{@code fcmaterials_journal.json} → {@link EddnMessage__6}</li>
 *   <li>{@code fssallbodiesfound.json}   → {@link EddnMessage__7}</li>
 *   <li>{@code fssbodysignals.json}      → {@link EddnMessage__8}</li>
 *   <li>{@code fssdiscoveryscan.json}    → {@link EddnMessage__9}</li>
 *   <li>{@code fsssignaldiscovered.json} → {@link EddnMessage__10}</li>
 *   <li>{@code journal.json}             → {@link EddnMessage__11}</li>
 *   <li>{@code navbeaconscan.json}       → {@link EddnMessage__12}</li>
 *   <li>{@code navroute.json}            → {@link EddnMessage__13}</li>
 *   <li>{@code outfitting.json}          → {@link EddnMessage__14}</li>
 *   <li>{@code scanbarycentre.json}      → {@link EddnMessage__15}</li>
 *   <li>{@code shipyard.json}            → {@link EddnMessage__16}</li>
 * </ul>
 */
public final class EddnMessages {

    private EddnMessages() {}

    /** Corps EDDN conforme au schéma {@code approachsettlement/1}. */
    public static final class ApproachSettlement extends EddnMessage {}

    /** Corps EDDN conforme au schéma {@code codexentry/1}. */
    public static final class CodexEntry extends EddnMessage__1 {}

    /** Corps EDDN conforme au schéma {@code commodity/3}. */
    public static final class Commodity extends EddnMessage__2 {}

    /** Corps EDDN conforme au schéma {@code dockingdenied/1}. */
    public static final class DockingDenied extends EddnMessage__3 {}

    /** Corps EDDN conforme au schéma {@code dockinggranted/1}. */
    public static final class DockingGranted extends EddnMessage__4 {}

    /** Corps EDDN conforme au schéma {@code fcmaterials_journal/1}. */
    public static final class FCMaterialsJournal extends EddnMessage__6 {}

    /** Corps EDDN conforme au schéma {@code fssallbodiesfound/1}. */
    public static final class FSSAllBodiesFound extends EddnMessage__7 {}

    /** Corps EDDN conforme au schéma {@code fssbodysignals/1}. */
    public static final class FSSBodySignals extends EddnMessage__8 {}

    /** Corps EDDN conforme au schéma {@code fssdiscoveryscan/1}. */
    public static final class FSSDiscoveryScan extends EddnMessage__9 {}

    /** Corps EDDN conforme au schéma {@code fsssignaldiscovered/1}. */
    public static final class FSSSignalDiscovered extends EddnMessage__10 {}

    /** Corps EDDN conforme au schéma {@code journal/1}. */
    public static final class Journal extends EddnMessage__11 {}

    /** Corps EDDN conforme au schéma {@code navbeaconscan/1}. */
    public static final class NavBeaconScan extends EddnMessage__12 {}

    /** Corps EDDN conforme au schéma {@code navroute/1}. */
    public static final class NavRoute extends EddnMessage__13 {}

    /** Corps EDDN conforme au schéma {@code outfitting/2}. */
    public static final class Outfitting extends EddnMessage__14 {}

    /** Corps EDDN conforme au schéma {@code scanbarycentre/1}. */
    public static final class ScanBaryCentre extends EddnMessage__15 {}

    /** Corps EDDN conforme au schéma {@code shipyard/2}. */
    public static final class Shipyard extends EddnMessage__16 {}
}
