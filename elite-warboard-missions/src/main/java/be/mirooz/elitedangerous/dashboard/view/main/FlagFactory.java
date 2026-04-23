package be.mirooz.elitedangerous.dashboard.view.main;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

/**
 * Petite fabrique de drapeaux dessinés en JavaFX (sans assets externes).
 * Les drapeaux sont stylisés mais reconnaissables.
 */
public final class FlagFactory {

    private static final double FLAG_W = 24;
    private static final double FLAG_H = 16;

    private FlagFactory() {
    }

    /**
     * Construit un drapeau pour le code langue donné (fr, en, de, es, it).
     */
    public static Node createFlag(String code) {
        if (code == null) {
            return placeholder();
        }
        switch (code.toLowerCase()) {
            case "fr":
                return createFrenchFlag();
            case "en":
                return createUnionJackFlag();
            case "de":
                return createGermanFlag();
            case "es":
                return createSpanishFlag();
            case "it":
                return createItalianFlag();
            default:
                return placeholder();
        }
    }

    private static Node placeholder() {
        Rectangle r = new Rectangle(FLAG_W, FLAG_H, Color.GRAY);
        return wrap(r);
    }

    private static Node wrap(Node content) {
        StackPane sp = new StackPane(content);
        sp.setMinSize(FLAG_W, FLAG_H);
        sp.setPrefSize(FLAG_W, FLAG_H);
        sp.setMaxSize(FLAG_W, FLAG_H);
        sp.setStyle("-fx-border-color: rgba(0,0,0,0.5); -fx-border-width: 1;");
        return sp;
    }

    private static Node createFrenchFlag() {
        HBox box = new HBox();
        box.setAlignment(Pos.CENTER);
        box.getChildren().addAll(
                stripe(FLAG_W / 3.0, FLAG_H, Color.web("#0055A4")),
                stripe(FLAG_W / 3.0, FLAG_H, Color.WHITE),
                stripe(FLAG_W / 3.0, FLAG_H, Color.web("#EF4135"))
        );
        return wrap(box);
    }

    private static Node createItalianFlag() {
        HBox box = new HBox();
        box.setAlignment(Pos.CENTER);
        box.getChildren().addAll(
                stripe(FLAG_W / 3.0, FLAG_H, Color.web("#009246")),
                stripe(FLAG_W / 3.0, FLAG_H, Color.WHITE),
                stripe(FLAG_W / 3.0, FLAG_H, Color.web("#CE2B37"))
        );
        return wrap(box);
    }

    private static Node createGermanFlag() {
        VBox box = new VBox();
        box.setAlignment(Pos.CENTER);
        box.getChildren().addAll(
                stripeH(FLAG_W, FLAG_H / 3.0, Color.BLACK),
                stripeH(FLAG_W, FLAG_H / 3.0, Color.web("#DD0000")),
                stripeH(FLAG_W, FLAG_H / 3.0, Color.web("#FFCE00"))
        );
        return wrap(box);
    }

    private static Node createSpanishFlag() {
        // 1/4 rouge, 1/2 jaune, 1/4 rouge (coat of arms ignoré pour simplicité)
        VBox box = new VBox();
        box.setAlignment(Pos.CENTER);
        box.getChildren().addAll(
                stripeH(FLAG_W, FLAG_H * 0.25, Color.web("#AA151B")),
                stripeH(FLAG_W, FLAG_H * 0.50, Color.web("#F1BF00")),
                stripeH(FLAG_W, FLAG_H * 0.25, Color.web("#AA151B"))
        );
        return wrap(box);
    }

    private static Node createUnionJackFlag() {
        Pane pane = new Pane();
        pane.setMinSize(FLAG_W, FLAG_H);
        pane.setPrefSize(FLAG_W, FLAG_H);
        pane.setMaxSize(FLAG_W, FLAG_H);

        Rectangle bg = new Rectangle(0, 0, FLAG_W, FLAG_H);
        bg.setFill(Color.web("#012169"));
        pane.getChildren().add(bg);

        // Diagonales blanches
        Line wd1 = new Line(0, 0, FLAG_W, FLAG_H);
        wd1.setStroke(Color.WHITE);
        wd1.setStrokeWidth(4);
        Line wd2 = new Line(0, FLAG_H, FLAG_W, 0);
        wd2.setStroke(Color.WHITE);
        wd2.setStrokeWidth(4);
        pane.getChildren().addAll(wd1, wd2);

        // Diagonales rouges (plus fines, par-dessus le blanc)
        Line rd1 = new Line(0, 0, FLAG_W, FLAG_H);
        rd1.setStroke(Color.web("#C8102E"));
        rd1.setStrokeWidth(1.5);
        Line rd2 = new Line(0, FLAG_H, FLAG_W, 0);
        rd2.setStroke(Color.web("#C8102E"));
        rd2.setStrokeWidth(1.5);
        pane.getChildren().addAll(rd1, rd2);

        // Croix blanche centrale (horizontale + verticale)
        Rectangle wh = new Rectangle(0, FLAG_H / 2.0 - 3, FLAG_W, 6);
        wh.setFill(Color.WHITE);
        Rectangle wv = new Rectangle(FLAG_W / 2.0 - 3, 0, 6, FLAG_H);
        wv.setFill(Color.WHITE);
        pane.getChildren().addAll(wh, wv);

        // Croix rouge centrale par-dessus
        Rectangle rh = new Rectangle(0, FLAG_H / 2.0 - 1.5, FLAG_W, 3);
        rh.setFill(Color.web("#C8102E"));
        Rectangle rv = new Rectangle(FLAG_W / 2.0 - 1.5, 0, 3, FLAG_H);
        rv.setFill(Color.web("#C8102E"));
        pane.getChildren().addAll(rh, rv);

        return wrap(pane);
    }

    private static Region stripe(double w, double h, Color c) {
        Region r = new Region();
        r.setMinSize(w, h);
        r.setPrefSize(w, h);
        r.setMaxSize(w, h);
        r.setStyle("-fx-background-color: " + toRgba(c) + ";");
        return r;
    }

    private static Region stripeH(double w, double h, Color c) {
        Region r = new Region();
        r.setMinSize(w, h);
        r.setPrefSize(w, h);
        r.setMaxSize(w, h);
        r.setStyle("-fx-background-color: " + toRgba(c) + ";");
        return r;
    }

    private static String toRgba(Color c) {
        return String.format("rgba(%d,%d,%d,%.2f)",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255),
                c.getOpacity());
    }
}
