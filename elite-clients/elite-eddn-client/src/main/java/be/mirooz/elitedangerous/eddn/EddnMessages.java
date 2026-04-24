package be.mirooz.elitedangerous.eddn;

import be.mirooz.elitedangerous.eddn.generated.EddnMessage__3;
import be.mirooz.elitedangerous.eddn.generated.EddnMessage__4;

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
 * ({@code additionalProperties: false}).</p>
 *
 * <p>Mapping schéma → classe mère (par ordre alphabétique des fichiers sous {@code eddn-schemas/}) :</p>
 * <ul>
 *   <li>{@code dockingdenied.json}  → {@link EddnMessage__3}</li>
 *   <li>{@code dockinggranted.json} → {@link EddnMessage__4}</li>
 * </ul>
 */
public final class EddnMessages {

    private EddnMessages() {}

    /** Corps EDDN conforme au schéma {@code dockingdenied/1}. */
    public static final class DockingDenied extends EddnMessage__3 {}

    /** Corps EDDN conforme au schéma {@code dockinggranted/1}. */
    public static final class DockingGranted extends EddnMessage__4 {}
}
