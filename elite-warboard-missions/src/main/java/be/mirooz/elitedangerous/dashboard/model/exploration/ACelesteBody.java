package be.mirooz.elitedangerous.dashboard.model.exploration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
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
public abstract class ACelesteBody {

    protected String timestamp;

    @JsonIgnore
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
