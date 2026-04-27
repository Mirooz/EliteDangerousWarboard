package be.mirooz.elitedangerous.dashboard.view.common.overlay;

import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;

/**
 * Glyphes et libellés communs pour les boutons d’overlay (même style que nav route / prospecteur / cibles).
 */
public final class OverlayUi {

    /** Icône « fenêtre / overlay » (identique au reste du projet). */
    public static final String GLYPH_WINDOW_STACK = "🗔";

    /** Cadenas ouvert : pas de clic à travers, interaction avec l’overlay. */
    public static final String GLYPH_LOCK_OPEN = "🔓";

    /** Cadenas fermé : clic à travers activé. */
    public static final String GLYPH_LOCK_CLOSED = "🔒";

    private OverlayUi() {
    }

    /** Texte du bouton overlay : icône + libellé localisé (ex. « … Overlay »). */
    public static String overlayActionLabel(String localizedOpenOrClose) {
        return GLYPH_WINDOW_STACK + " " + localizedOpenOrClose;
    }

    public static void updateLockToggleGlyph(ToggleButton lockButton) {
        if (lockButton == null) {
            return;
        }
        lockButton.setText(lockButton.isSelected() ? GLYPH_LOCK_CLOSED : GLYPH_LOCK_OPEN);
    }

    /**
     * Style du verrou « clic à travers » : cadre {@code elite-nav-button} + typo adaptée aux emoji
     * (même rendu partout : cibles, route, prospecteur, corps, fleet…).
     */
    public static void applyOverlayLockToggleStyle(ToggleButton lockButton) {
        if (lockButton == null) {
            return;
        }
        if (!lockButton.getStyleClass().contains("elite-nav-button")) {
            lockButton.getStyleClass().add("elite-nav-button");
        }
        if (!lockButton.getStyleClass().contains("overlay-pass-through-lock")) {
            lockButton.getStyleClass().add("overlay-pass-through-lock");
        }
    }

    public static void refreshLockTooltip(ToggleButton lockButton, LocalizationService localizationService) {
        if (lockButton == null) {
            return;
        }
        Tooltip t = lockButton.getTooltip();
        if (t == null) {
            return;
        }
        t.setText(lockButton.isSelected()
                ? localizationService.getString("overlay.lock.tooltipOn")
                : localizationService.getString("overlay.lock.tooltipOff"));
    }
}
