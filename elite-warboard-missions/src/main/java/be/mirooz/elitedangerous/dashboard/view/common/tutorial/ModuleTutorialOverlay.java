package be.mirooz.elitedangerous.dashboard.view.common.tutorial;

import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Visite guidée par étapes au-dessus d’un module : assombrissement, cadre sur la zone courante, carte texte.
 */
public final class ModuleTutorialOverlay {

    public record TutorialStep(String titleKey, String bodyKey, Supplier<Node> targetSupplier) {}

    private static final double HIGHLIGHT_PAD = 8;
    private static final double CARD_MIN_WIDTH = 320;
    private static final double CARD_MAX_WIDTH = 420;

    private final StackPane host;
    private final LocalizationService localizationService;

    private final StackPane glass = new StackPane();
    private final Pane positioningLayer = new Pane();
    private final Rectangle dimmer = new Rectangle();
    private final StackPane highlightRing = new StackPane();
    private final VBox card = new VBox(10);
    private final Label titleLabel = new Label();
    private final Label bodyLabel = new Label();
    private final Button btnCloseCorner = new Button("✕");
    private final Button btnPrev = new Button();
    private final Button btnNext = new Button();
    private final ScrollPane bodyScroll = new ScrollPane(bodyLabel);

    private List<TutorialStep> steps = List.of();
    private int index;
    private Node watchedTarget;
    private final InvalidationListener layoutRelayout = obs -> requestRelayout();
    private final javafx.beans.value.ChangeListener<? super Number> sizeRelayout = (obs, o, n) -> requestRelayout();

    public ModuleTutorialOverlay(StackPane host, LocalizationService localizationService) {
        this.host = host;
        this.localizationService = localizationService;
        buildUi();
    }

    private void buildUi() {
        glass.getStyleClass().add("tutorial-glass");
        glass.setMaxWidth(Double.MAX_VALUE);
        glass.setMaxHeight(Double.MAX_VALUE);
        glass.setFocusTraversable(true);
        glass.addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);

        dimmer.getStyleClass().add("tutorial-dimmer");
        dimmer.setFill(Color.rgb(5, 5, 5, 0.62));
        dimmer.setMouseTransparent(false);
        dimmer.addEventFilter(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);

        highlightRing.getStyleClass().add("tutorial-highlight-ring");
        highlightRing.setMouseTransparent(true);

        titleLabel.getStyleClass().add("tutorial-card-title");
        titleLabel.setWrapText(true);
        bodyLabel.getStyleClass().add("tutorial-card-body");
        bodyLabel.setWrapText(true);
        bodyScroll.setFitToWidth(true);
        bodyScroll.setPrefViewportHeight(160);
        bodyScroll.setMaxHeight(240);
        bodyScroll.getStyleClass().add("tutorial-card-scroll");
        bodyScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        btnCloseCorner.getStyleClass().addAll("tutorial-card-close", "elite-nav-button");
        btnCloseCorner.setOnAction(e -> stop());
        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        titleRow.getChildren().addAll(titleLabel, btnCloseCorner);

        btnPrev.getStyleClass().add("elite-nav-button");
        btnNext.getStyleClass().addAll("elite-nav-button", "tutorial-card-primary");
        btnPrev.setOnAction(e -> goRelative(-1));
        btnNext.setOnAction(e -> goRelative(1));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox nav = new HBox(10, btnPrev, spacer, btnNext);
        nav.setAlignment(Pos.CENTER_LEFT);
        nav.setPadding(new Insets(4, 0, 0, 0));

        card.getChildren().addAll(titleRow, bodyScroll, nav);
        card.getStyleClass().add("tutorial-card");
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setMaxWidth(CARD_MAX_WIDTH);
        card.setMinWidth(CARD_MIN_WIDTH);
        card.setMouseTransparent(false);
        card.addEventFilter(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);

        positioningLayer.setMinSize(0, 0);
        positioningLayer.prefWidthProperty().bind(host.widthProperty());
        positioningLayer.prefHeightProperty().bind(host.heightProperty());
        positioningLayer.maxWidthProperty().bind(host.widthProperty());
        positioningLayer.maxHeightProperty().bind(host.heightProperty());
        positioningLayer.getChildren().addAll(dimmer, highlightRing, card);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(host.widthProperty());
        clip.heightProperty().bind(host.heightProperty());
        positioningLayer.setClip(clip);

        StackPane.setAlignment(positioningLayer, Pos.TOP_LEFT);
        dimmer.widthProperty().bind(host.widthProperty());
        dimmer.heightProperty().bind(host.heightProperty());

        glass.getChildren().add(positioningLayer);
    }

    private void onKeyPressed(KeyEvent e) {
        if (e.getCode() == KeyCode.ESCAPE) {
            stop();
            e.consume();
        }
    }

    public boolean isShowing() {
        return host.getChildren().contains(glass);
    }

    public void start(List<TutorialStep> newSteps) {
        if (newSteps == null || newSteps.isEmpty()) {
            return;
        }
        stop();
        this.steps = new ArrayList<>(newSteps);
        this.index = 0;
        host.getChildren().add(glass);
        host.widthProperty().addListener(sizeRelayout);
        host.heightProperty().addListener(sizeRelayout);
        attachHostLayoutListener();
        refreshLocalizedChrome();
        showStep(0);
        Platform.runLater(() -> {
            glass.requestFocus();
            relayout();
        });
    }

    private void attachHostLayoutListener() {
        host.layoutBoundsProperty().addListener(layoutRelayout);
    }

    private void detachHostLayoutListener() {
        host.layoutBoundsProperty().removeListener(layoutRelayout);
    }

    public void stop() {
        detachTargetListeners();
        host.widthProperty().removeListener(sizeRelayout);
        host.heightProperty().removeListener(sizeRelayout);
        detachHostLayoutListener();
        host.getChildren().remove(glass);
        steps = List.of();
        index = 0;
    }

    public void refreshStepTextIfVisible() {
        if (!isShowing() || steps.isEmpty()) {
            return;
        }
        refreshLocalizedChrome();
        applyStepTexts(steps.get(index));
    }

    private void refreshLocalizedChrome() {
        btnPrev.setText(localizationService.getString("tutorial.previous"));
        btnNext.setText(localizationService.getString("tutorial.next"));
        btnCloseCorner.setTooltip(new Tooltip(localizationService.getString("tutorial.close")));
    }

    private void goRelative(int delta) {
        int next = index + delta;
        if (next < 0) {
            return;
        }
        if (next >= steps.size()) {
            stop();
            return;
        }
        showStep(next);
    }

    private void showStep(int i) {
        index = i;
        TutorialStep step = steps.get(index);
        detachTargetListeners();
        applyStepTexts(step);
        Node target = step.targetSupplier().get();
        watchedTarget = target;
        if (target != null) {
            target.layoutBoundsProperty().addListener(layoutRelayout);
            target.boundsInParentProperty().addListener(layoutRelayout);
        }
        btnPrev.setDisable(index <= 0);
        boolean last = index >= steps.size() - 1;
        btnNext.setText(last
                ? localizationService.getString("tutorial.finish")
                : localizationService.getString("tutorial.next"));
        relayout();
        Platform.runLater(() -> {
            glass.requestFocus();
            relayout();
        });
    }

    private void applyStepTexts(TutorialStep step) {
        titleLabel.setText(localizationService.getString(step.titleKey()));
        bodyLabel.setText(localizationService.getString(step.bodyKey()));
    }

    private void detachTargetListeners() {
        if (watchedTarget != null) {
            watchedTarget.layoutBoundsProperty().removeListener(layoutRelayout);
            watchedTarget.boundsInParentProperty().removeListener(layoutRelayout);
            watchedTarget = null;
        }
    }

    private void requestRelayout() {
        Platform.runLater(this::relayout);
    }

    private void relayout() {
        if (!isShowing() || steps.isEmpty()) {
            return;
        }
        double w = host.getWidth();
        double h = host.getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        TutorialStep step = steps.get(index);
        Node target = step.targetSupplier().get();
        if (target == null || target.getScene() == null) {
            highlightRing.setVisible(false);
            positionCardCenteredBelow(w, h, null);
            return;
        }
        highlightRing.setVisible(true);
        var sceneBounds = target.localToScene(target.getBoundsInLocal());
        var inHost = host.sceneToLocal(sceneBounds);
        double hx = Math.max(0, inHost.getMinX() - HIGHLIGHT_PAD);
        double hy = Math.max(0, inHost.getMinY() - HIGHLIGHT_PAD);
        double hw = Math.min(w - hx, inHost.getWidth() + 2 * HIGHLIGHT_PAD);
        double hh = Math.min(h - hy, inHost.getHeight() + 2 * HIGHLIGHT_PAD);
        highlightRing.setLayoutX(hx);
        highlightRing.setLayoutY(hy);
        highlightRing.setPrefSize(Math.max(0, hw), Math.max(0, hh));
        highlightRing.setMinSize(Math.max(0, hw), Math.max(0, hh));
        highlightRing.setMaxSize(Math.max(0, hw), Math.max(0, hh));
        positionCardCenteredBelow(w, h, new double[]{hx, hy, hw, hh});
    }

    /** Place la carte sous le surlignage, ou au-dessus / centré si manque de place. */
    private void positionCardCenteredBelow(double hostW, double hostH, double[] highlightBox) {
        card.applyCss();
        card.layout();
        double cw = Math.min(CARD_MAX_WIDTH, Math.max(CARD_MIN_WIDTH, card.prefWidth(-1)));
        double ch = card.prefHeight(cw);
        double margin = 16;
        double cx = (hostW - cw) / 2;
        double cy;
        if (highlightBox != null) {
            double belowY = highlightBox[1] + highlightBox[3] + margin;
            double aboveY = highlightBox[1] - ch - margin;
            if (belowY + ch <= hostH - margin) {
                cy = belowY;
            } else if (aboveY >= margin) {
                cy = aboveY;
            } else {
                cy = Math.max(margin, Math.min(hostH - ch - margin, (hostH - ch) / 2));
            }
        } else {
            cy = Math.max(margin, Math.min(hostH - ch - margin, (hostH - ch) / 2));
        }
        cx = Math.max(margin, Math.min(hostW - cw - margin, cx));
        cy = Math.max(margin, Math.min(hostH - ch - margin, cy));
        card.setLayoutX(cx);
        card.setLayoutY(cy);
        card.setPrefWidth(cw);
    }
}
