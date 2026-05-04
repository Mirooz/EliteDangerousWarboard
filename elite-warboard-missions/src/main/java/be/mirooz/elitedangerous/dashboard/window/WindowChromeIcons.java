package be.mirooz.elitedangerous.dashboard.window;

import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

/** Glyphes vectoriels pour les boutons de la barre titre custom. */
public final class WindowChromeIcons {

    private static final double ICON = 14.0;

    private WindowChromeIcons() {
    }

    public static StackPane minimize() {
        Line bar = new Line(2, 7, 12, 7);
        bar.getStyleClass().add("window-chrome-icon-line");
        bar.setStrokeWidth(2);
        return wrap(bar);
    }

    public static StackPane maximize() {
        Rectangle r = new Rectangle(3, 3, 8, 8);
        r.setFill(javafx.scene.paint.Color.TRANSPARENT);
        r.getStyleClass().add("window-chrome-icon-rect");
        r.setStrokeWidth(1.5);
        return wrap(r);
    }

    public static StackPane restore() {
        Rectangle back = new Rectangle(5, 3, 7, 7);
        back.setFill(javafx.scene.paint.Color.TRANSPARENT);
        back.getStyleClass().add("window-chrome-icon-rect");
        back.setStrokeWidth(1.3);

        Rectangle front = new Rectangle(2, 5, 7, 7);
        front.setFill(javafx.scene.paint.Color.TRANSPARENT);
        front.getStyleClass().add("window-chrome-icon-rect");
        front.setStrokeWidth(1.3);

        return wrap(new Group(back, front));
    }

    public static StackPane close() {
        Line a = new Line(3, 3, 11, 11);
        a.getStyleClass().addAll("window-chrome-icon-line", "window-chrome-icon-close-line");
        a.setStrokeWidth(2);

        Line b = new Line(11, 3, 3, 11);
        b.getStyleClass().addAll("window-chrome-icon-line", "window-chrome-icon-close-line");
        b.setStrokeWidth(2);

        return wrap(new Group(a, b));
    }

    private static StackPane wrap(Node node) {
        StackPane p = new StackPane(node);
        p.setMinSize(ICON, ICON);
        p.setPrefSize(ICON, ICON);
        p.setMaxSize(ICON, ICON);
        p.setAlignment(Pos.CENTER);
        return p;
    }
}
