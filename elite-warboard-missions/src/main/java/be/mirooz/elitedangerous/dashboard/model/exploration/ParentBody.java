package be.mirooz.elitedangerous.dashboard.model.exploration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Représente un parent d'un corps céleste.
 * Un parent peut être une planète, une étoile, ou null (point de référence).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParentBody {
    /**
     * Type du parent : "Planet", "Star", "Null", etc.
     */
    private String type;
    
    /**
     * Identifiant du corps parent (BodyID)
     */
    private int bodyID;
}

