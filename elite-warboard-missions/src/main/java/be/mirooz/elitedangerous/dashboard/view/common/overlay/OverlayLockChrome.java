package be.mirooz.elitedangerous.dashboard.view.common.overlay;

import javafx.scene.Node;
import javafx.scene.layout.StackPane;

/**
 * Cadre orange et contrôles d’overlay (opacité, redimensionnement, échelle du texte) selon le verrou clic à travers.
 */
public final class OverlayLockChrome {

    private static final double CHROME_VISIBLE_OPACITY = 0.88;

    private OverlayLockChrome() {
    }

    /**
     * @param locked {@code true} : clic à travers — pas de cadre orange, chrome masqué.
     *               {@code false} : interaction — cadre {@code overlay-root-bordered} + contrôles visibles.
     */
    public static void apply(boolean locked, StackPane stackPane, Node... chrome) {
        if (stackPane != null) {
            if (locked) {
                stackPane.getStyleClass().remove("overlay-root-bordered");
            } else if (!stackPane.getStyleClass().contains("overlay-root-bordered")) {
                stackPane.getStyleClass().add("overlay-root-bordered");
            }
        }
        for (Node n : chrome) {
            if (n == null) {
                continue;
            }
            n.setVisible(!locked);
            n.setManaged(!locked);
            if (!locked) {
                n.setOpacity(CHROME_VISIBLE_OPACITY);
            }
        }
    }
}
