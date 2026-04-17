package be.mirooz.elitedangerous.dashboard.model.colonisation;

/**
 * État de chantier dérivé des booléens {@code ConstructionComplete} / {@code ConstructionFailed} du journal.
 */
public enum ConstructionStatus {

    /** {@code ConstructionComplete} true (prioritaire si les deux étaient true par erreur) */
    COMPLETE,

    /** {@code ConstructionFailed} true */
    FAILED,

    /** Ni terminé ni échoué */
    IN_PROGRESS;

    public static ConstructionStatus fromJournalBooleans(boolean constructionComplete, boolean constructionFailed) {
        if (constructionComplete) {
            return COMPLETE;
        }
        if (constructionFailed) {
            return FAILED;
        }
        return IN_PROGRESS;
    }
}
