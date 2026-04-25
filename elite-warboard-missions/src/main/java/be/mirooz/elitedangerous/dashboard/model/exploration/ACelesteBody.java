package be.mirooz.elitedangerous.dashboard.model.exploration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe abstraite commune aux planètes, étoiles ou tout autre corps céleste scanné.
 * Contient toutes les propriétés partagées.
 *
 * <p>{@link NoArgsConstructor} : requis pour la désérialisation Jackson. {@link SuperBuilder}
 * seul ne génère qu'un constructeur protégé prenant un builder en paramètre, ce qui n'est pas
 * utilisable par Jackson comme creator par défaut. Le no-arg constructor est appelé puis
 * Jackson remplit les fields via reflection.</p>
 */
@Data
@SuperBuilder
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = StarDetail.class, name = "star"),
        @JsonSubTypes.Type(value = PlaneteDetail.class, name = "planet")
})
public abstract class ACelesteBody {

    protected String timestamp;

    // JsonNode est un type natif Jackson : il est (dé)sérialisé directement en arbre JSON
    // sans subir le default-typing polymorphique. Conservé pour permettre l'affichage
    // "JSON brut" du body dans la vue exploration.
    protected JsonNode jsonNode;
    protected Double lsDistance;
    // Informations de base
    protected String bodyName;
    protected String starSystem;
    protected boolean mapped;
    @Builder.Default
    protected boolean efficiencyTargetMap =true;
    protected long systemAddress;
    protected int bodyID;
    protected boolean rings;
    // Parents (hiérarchie orbitale)
    @Builder.Default
    protected List<ParentBody> parents = new ArrayList<>();

    // Statut de découverte
    protected boolean wasMapped;
    protected boolean wasFootfalled;
    protected boolean wasDiscovered;

    public abstract long computeBodyValue();
}
