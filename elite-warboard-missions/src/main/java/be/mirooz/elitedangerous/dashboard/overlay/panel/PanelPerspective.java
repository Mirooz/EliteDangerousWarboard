package be.mirooz.elitedangerous.dashboard.overlay.panel;

import javafx.scene.effect.PerspectiveTransform;

/**
 * Pont entre la détection OpenCV (coins en pixels) et {@link PerspectiveTransform}.
 * <p>
 * <strong>Pourquoi pas {@link javafx.scene.transform.Affine} ?</strong> Une affine 2D
 * conserve le parallélisme : un rectangle reste un parallélogramme. Le panneau cockpit
 * est un trapèze (côtés non parallèles en projection) : il faut une <em>homographie</em>,
 * modélisée en JavaFX par {@link PerspectiveTransform}.
 * <p>
 * Chaîne cible (hors périmètre de cette démo) : snapshot du {@code Pane} Warboard réel
 * → {@link javafx.scene.image.ImageView} rectangulaire → ce transform → quadrilatère aligné sur le cockpit.
 * <p>
 * Les propriétés {@code ulx…lly} décrivent la position <em>de sortie</em> des quatre coins
 * du contenu rectangulaire source (voir javadoc {@link PerspectiveTransform}) ; en pratique,
 * on les fixe aux coins {@link PanelCorners} mesurés sur la capture cockpit, dans le même
 * repère pixel que l’{@link javafx.scene.image.ImageView} si celle-ci occupe la même grille que l’image.
 */
public final class PanelPerspective {

    private PanelPerspective() {
    }

    /**
     * Crée un effet prêt à être posé sur une {@link javafx.scene.image.ImageView} ({@code input} laissé à
     * {@code null} : JavaFX utilise alors le rendu du nœud comme entrée).
     */
    public static PerspectiveTransform createForQuad(PanelCorners quad) {
        PerspectiveTransform pt = new PerspectiveTransform();
        applyQuadToPerspectiveTransform(pt, quad);
        return pt;
    }

    public static void applyQuadToPerspectiveTransform(PerspectiveTransform pt, PanelCorners quad) {
        pt.setUlx(quad.topLeft().getX());
        pt.setUly(quad.topLeft().getY());
        pt.setUrx(quad.topRight().getX());
        pt.setUry(quad.topRight().getY());
        pt.setLrx(quad.bottomRight().getX());
        pt.setLry(quad.bottomRight().getY());
        pt.setLlx(quad.bottomLeft().getX());
        pt.setLly(quad.bottomLeft().getY());
    }
}
