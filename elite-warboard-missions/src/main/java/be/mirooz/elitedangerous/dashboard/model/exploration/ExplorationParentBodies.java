package be.mirooz.elitedangerous.dashboard.model.exploration;

/**
 * Utilitaires sur les entrées {@link ParentBody} du journal / Spansh.
 */
public final class ExplorationParentBodies {

    private ExplorationParentBodies() {
    }

    /**
     * Référentiel « racine » du système : parent {@code Null} (repère journal) ou barycentre binaire.
     * Ces liens ne matérialisent pas un corps affichable dans l’orrery : l’étoile qui n’a que ce type
     * de parents se comporte comme soleil principal.
     */
    public static boolean isRootReferenceParent(ParentBody p) {
        if (p == null) {
            return false;
        }
        if ("Null".equalsIgnoreCase(p.getType())) {
            return true;
        }
        return isBarycentreType(p.getType());
    }

    public static boolean isBarycentreType(String type) {
        if (type == null) {
            return false;
        }
        return type.equalsIgnoreCase("Barycentre") || type.equalsIgnoreCase("BaryCentre");
    }
}
