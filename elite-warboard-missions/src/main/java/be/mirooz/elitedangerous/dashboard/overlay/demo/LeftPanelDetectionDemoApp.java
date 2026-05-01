package be.mirooz.elitedangerous.dashboard.overlay.demo;

import be.mirooz.elitedangerous.dashboard.overlay.panel.LeftPanelCornerDetector;
import be.mirooz.elitedangerous.dashboard.overlay.panel.PanelCorners;
import be.mirooz.elitedangerous.dashboard.overlay.panel.PanelPerspective;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.effect.PerspectiveTransform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import nu.pattern.OpenCV;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Optional;

/**
 * Démo : charge {@code /images/overlay/leftpanel.bmp}, détecte le quadrilatère orange (OpenCV),
 * affiche coins + lignes + libellés, texte des coordonnées, et une prévisualisation
 * {@link PerspectiveTransform} (sans capture temps réel ni interaction).
 * <p>
 * Lancement (recommandé, compatible PowerShell) :
 * <ul>
 *   <li>racine du dépôt : {@code mvn -pl elite-warboard-missions exec:java@left-panel-opencv-demo}</li>
 *   <li>dossier {@code elite-warboard-missions/} : {@code mvn exec:java@left-panel-opencv-demo}</li>
 * </ul>
 * Sous PowerShell, éviter {@code -Dexec.mainClass=...} sans guillemets : le point dans {@code exec.mainClass}
 * peut faire que l’argument soit tronqué et que Maven interprète {@code .mainClass=...} comme une phase.
 * Alternative : {@code mvn exec:java "-Dexec.mainClass=be.mirooz.elitedangerous.dashboard.overlay.demo.LeftPanelDetectionDemoApp"}
 */
public class LeftPanelDetectionDemoApp extends Application {

    private static final String RESOURCE_IMAGE = "/images/overlay/leftpanel.bmp";

    static {
        OpenCV.loadLocally();
    }

    @Override
    public void start(Stage stage) throws IOException {
        byte[] raw;
        try (InputStream in = LeftPanelDetectionDemoApp.class.getResourceAsStream(RESOURCE_IMAGE)) {
            if (in == null) {
                throw new IllegalStateException("Ressource introuvable : " + RESOURCE_IMAGE);
            }
            raw = in.readAllBytes();
        }

        Mat buffer = new Mat(1, raw.length, CvType.CV_8UC1);
        buffer.put(0, 0, raw);
        Mat bgr = Imgcodecs.imdecode(buffer, Imgcodecs.IMREAD_COLOR);
        buffer.release();
        if (bgr.empty()) {
            throw new IllegalStateException("OpenCV n'a pas pu décoder l'image (imdecode).");
        }

        Optional<PanelCorners> cornersOpt = LeftPanelCornerDetector.detect(bgr);
        int decodedWidth = bgr.cols();
        int decodedHeight = bgr.rows();
        bgr.release();

        Image fxImage = new Image(new ByteArrayInputStream(raw));
        ImageView imageView = new ImageView(fxImage);
        imageView.setPreserveRatio(false);

        Canvas overlay = new Canvas();
        overlay.setMouseTransparent(true);

        StackPane imageStack = new StackPane(imageView, overlay);
        imageStack.setAlignment(Pos.TOP_LEFT);

        Runnable syncOverlaySize = () -> {
            overlay.setWidth(fxImage.getWidth());
            overlay.setHeight(fxImage.getHeight());
            imageView.setFitWidth(fxImage.getWidth());
            imageView.setFitHeight(fxImage.getHeight());
            cornersOpt.ifPresent(c -> drawOverlay(overlay.getGraphicsContext2D(), c));
        };

        imageView.imageProperty().addListener((obs, o, n) -> syncOverlaySize.run());
        syncOverlaySize.run();

        TextArea coords = new TextArea(buildCoordsText(cornersOpt, decodedWidth, decodedHeight));
        coords.setEditable(false);
        coords.setWrapText(true);
        coords.setPrefRowCount(14);
        coords.setFont(Font.font("Consolas", 12));

        VBox rightColumn = new VBox(10, new Label("Coordonnées & PerspectiveTransform"), coords);
        rightColumn.setPadding(new Insets(8));

        if (cornersOpt.isPresent()) {
            PanelCorners c = cornersOpt.get();
            ImageView warped = new ImageView(fxImage);
            warped.setFitWidth(fxImage.getWidth());
            warped.setFitHeight(fxImage.getHeight());
            PerspectiveTransform pt = PanelPerspective.createForQuad(c);
            warped.setEffect(pt);
            warped.setPreserveRatio(false);
            Label cap = new Label(
                    "Prévisualisation : même ImageView + PerspectiveTransform vers le quad détecté "
                            + "(repère pixel image ; Affine ne pourrait pas produire ce trapèze).");
            cap.setWrapText(true);
            StackPane previewStack = new StackPane(warped);
            VBox previewBox = new VBox(6, cap, previewStack);
            previewBox.setPadding(new Insets(0, 0, 8, 0));
            rightColumn.getChildren().add(previewBox);
        }

        ScrollPane scroll = new ScrollPane(imageStack);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);

        BorderPane root = new BorderPane();
        root.setCenter(scroll);
        root.setRight(rightColumn);
        BorderPane.setMargin(rightColumn, new Insets(8));

        Scene scene = new Scene(root, 1280, 800);
        stage.setTitle("Elite Warboard — détection panneau gauche (OpenCV + JavaFX)");
        stage.setScene(scene);
        stage.show();

        syncOverlaySize.run();
    }

    private static void drawOverlay(GraphicsContext gc, PanelCorners c) {
        gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
        gc.setLineWidth(2);
        gc.setStroke(Color.LIME);
        gc.strokePolygon(
                new double[]{c.topLeft().getX(), c.topRight().getX(), c.bottomRight().getX(), c.bottomLeft().getX()},
                new double[]{c.topLeft().getY(), c.topRight().getY(), c.bottomRight().getY(), c.bottomLeft().getY()},
                4
        );

        drawCorner(gc, c.topLeft(), "TL", Color.RED);
        drawCorner(gc, c.topRight(), "TR", Color.RED);
        drawCorner(gc, c.bottomRight(), "BR", Color.RED);
        drawCorner(gc, c.bottomLeft(), "BL", Color.RED);
    }

    private static void drawCorner(GraphicsContext gc, Point2D p, String label, Color dot) {
        double r = 5;
        gc.setFill(dot);
        gc.fillOval(p.getX() - r, p.getY() - r, 2 * r, 2 * r);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokeOval(p.getX() - r, p.getY() - r, 2 * r, 2 * r);

        gc.setFont(Font.font(14));
        double tx = p.getX() + 10;
        double ty = p.getY() - 8;
        gc.setLineWidth(3);
        gc.setStroke(Color.BLACK);
        gc.strokeText(label, tx, ty);
        gc.setFill(Color.WHITE);
        gc.fillText(label, tx, ty);
    }

    private static String buildCoordsText(Optional<PanelCorners> cornersOpt, int imgW, int imgH) {
        String intro = """
                Le panneau gauche cockpit est vu en perspective : le contour utile est un quadrilatère, pas un rectangle aligné aux axes.
                Une transformation Affine 2D ne suffit pas (elle ne produit qu'un parallélogramme). JavaFX expose l'homographie via PerspectiveTransform : on appliquera ce transform sur une ImageView alimentée par un snapshot du Pane Warboard pour « coller » l'UI dans le cockpit.

                Image : %d × %d px

                """.formatted(imgW, imgH);

        if (cornersOpt.isEmpty()) {
            return intro + "Aucun quadrilatère orange détecté (ajuster HSV dans LeftPanelCornerDetector si besoin).";
        }
        PanelCorners c = cornersOpt.get();
        String nl = System.lineSeparator();
        return intro
                + "Coins (TL → TR → BR → BL, repère image y vers le bas) :" + nl
                + pointLine("TL", c.topLeft()) + nl
                + pointLine("TR", c.topRight()) + nl
                + pointLine("BR", c.bottomRight()) + nl
                + pointLine("BL", c.bottomLeft()) + nl
                + nl
                + "Exemple JavaFX (à brancher sur l'ImageView du snapshot) :" + nl
                + "PerspectiveTransform pt = PanelPerspective.createForQuad(corners);" + nl
                + "imageView.setEffect(pt);" + nl;
    }

    private static String pointLine(String name, Point2D p) {
        return "%s : (%s, %s)".formatted(
                name,
                String.format(Locale.ROOT, "%.2f", p.getX()),
                String.format(Locale.ROOT, "%.2f", p.getY()));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
