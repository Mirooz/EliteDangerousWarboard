package be.mirooz.elitedangerous.dashboard.model.exploration;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe abstraite commune aux planètes, étoiles ou tout autre corps céleste scanné.
 * Contient toutes les propriétés partagées.
 */
@Data
@SuperBuilder
public abstract class ACelesteBody {

    protected String timestamp;

    protected JsonNode jsonNode;
    protected Double lsDistance;
    // Informations de base
    protected String bodyName;
    protected String starSystem;
    protected boolean mapped;
    protected boolean efficiencyTargetMap;
    protected long systemAddress;
    protected int bodyID;

    // Parents (hiérarchie orbitale)
    @Builder.Default
    protected List<ParentBody> parents = new ArrayList<>();

    // Statut de découverte
    protected boolean wasMapped;
    protected boolean wasFootfalled;
    protected boolean wasDiscovered;

    public abstract int computeValue();
}
