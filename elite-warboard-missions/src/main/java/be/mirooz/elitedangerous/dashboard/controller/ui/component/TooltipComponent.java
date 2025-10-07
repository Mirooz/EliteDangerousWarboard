package be.mirooz.elitedangerous.dashboard.controller.ui.component;

import javafx.scene.control.Tooltip;
import javafx.util.Duration;

public class TooltipComponent extends Tooltip {
    public TooltipComponent(String texte) {
        super(texte);
        this.setShowDelay(Duration.millis(300));
        this.setHideDelay(Duration.millis(100));
    }
}
