package be.mirooz.elitedangerous.dashboard.overlay.panel;

import javafx.geometry.Point2D;

/**
 * Les quatre coins du panneau gauche tel qu’il apparaît en perspective à l’écran.
 * <p>
 * Convention identique à {@link javafx.scene.effect.PerspectiveTransform} et au reste du
 * tableau de bord : haut-gauche, haut-droite, bas-droite, bas-gauche (sens horaire à partir
 * du coin supérieur gauche de l’image).
 */
public record PanelCorners(
        Point2D topLeft,
        Point2D topRight,
        Point2D bottomRight,
        Point2D bottomLeft
) {
}
