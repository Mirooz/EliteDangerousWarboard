package be.mirooz.elitedangerous.dashboard.overlay.panel;

/**
 * Valeurs de <strong>repli</strong> pour le quadrilatère du dock cockpit (coordonnées normalisées {@code (0–1, 0–1)}).
 * <p>
 * En production, {@link be.mirooz.elitedangerous.dashboard.service.CockpitLeftPanelGeometryService} charge
 * une image de calibration (tracé bleu), calcule les sommets avec OpenCV et met à jour les ratios
 * dynamiquement ; ce fichier conserve les constantes utilisées si OpenCV échoue ou si aucune image n’est
 * trouvée.
 * <p>
 * Écran cible du dock : {@link be.mirooz.elitedangerous.dashboard.service.CockpitDockPlacementService}
 * et préférence {@value #PREF_COCKPIT_DOCK_SCREEN_INDEX}.
 */
public final class CockpitLeftPanelGeometry {

    public static final int REFERENCE_CAPTURE_WIDTH = 2560;
    public static final int REFERENCE_CAPTURE_HEIGHT = 1440;

    /**
     * Clé préférences : index 0-based dans {@link javafx.stage.Screen#getScreens()} pour forcer
     * le moniteur du dock.
     */
    public static final String PREF_COCKPIT_DOCK_SCREEN_INDEX = "cockpit.dock.screen.index";

    /*
     * Repli aligné sur leftpanelposition2.bmp — 2560×1440 (dernier export manuel avant service dynamique).
     */
    private static final double W = REFERENCE_CAPTURE_WIDTH;
    private static final double H = REFERENCE_CAPTURE_HEIGHT;

    public static final double NORM_TOP_LEFT_X = 411.75665283203125 / W;
    public static final double NORM_TOP_LEFT_Y = 422.38177490234375 / H;
    public static final double NORM_TOP_RIGHT_X = 1693.5062255859375 / W;
    public static final double NORM_TOP_RIGHT_Y = 401.5139465332031 / H;
    public static final double NORM_BOTTOM_RIGHT_X = 1715.1593017578125 / W;
    public static final double NORM_BOTTOM_RIGHT_Y = 1030.6025390625 / H;
    public static final double NORM_BOTTOM_LEFT_X = 467.8109130859375 / W;
    public static final double NORM_BOTTOM_LEFT_Y = 1157.5322265625 / H;

    private CockpitLeftPanelGeometry() {
    }
}
