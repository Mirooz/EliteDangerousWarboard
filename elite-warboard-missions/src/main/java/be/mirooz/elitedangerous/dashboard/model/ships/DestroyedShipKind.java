package be.mirooz.elitedangerous.dashboard.model.ships;

/**
 * Type de victoire enregistrée (bounty PvE, bond de conflit, etc.).
 * Sérialisé en JSON sous la clé {@code "type"} pour rester aligné sur l'ancien format.
 */
public enum DestroyedShipKind {
    BOUNTY,
    CONFLICT,
    /** Valeur de repli si la saisie est inconnue (migration / robustesse). */
    UNKNOWN
}
