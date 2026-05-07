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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.beans.binding.Bindings;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Visite guidée par étapes au-dessus d’un module : assombrissement, cadre sur la zone courante, carte texte.
 */
public final class ModuleTutorialOverlay {

    /**
     * @param illustrationResourcePath chemin classpath sous {@code /} vers une image (ex. {@code /images/tutorial/foo.png}), ou {@code null}
     */
    public record TutorialStep(String titleKey, String bodyKey, Supplier<Node> targetSupplier, String illustrationResourcePath) {
        public TutorialStep(String titleKey, String bodyKey, Supplier<Node> targetSupplier) {
            this(titleKey, bodyKey, targetSupplier, null);
        }
    }

    private static final double HIGHLIGHT_PAD = 8;
    private static final double CARD_MIN_WIDTH = 320;
    private static final double CARD_MAX_WIDTH = 420;
    /** Étape 1 (texte long, sans capture) : carte plus large que le mode par défaut. */
    private static final double CARD_MIN_WIDTH_STEP1 = 500;
    private static final double CARD_MAX_WIDTH_STEP1 = 720;
    /** Carte élargie quand une capture d’écran est affichée (tableau large recherche missions). */
    private static final double CARD_MIN_WIDTH_WIDE = 540;
    private static final double CARD_MAX_WIDTH_WIDE = 820;

    private boolean tutorialCardWide;
    private boolean tutorialCardStep1Wide;

    private final StackPane host;
    private final LocalizationService localizationService;

    private final StackPane glass = new StackPane();
    private final Pane positioningLayer = new Pane();
    private final Rectangle dimmer = new Rectangle();
    private final StackPane highlightRing = new StackPane();
    private final VBox card = new VBox(10);
    private final Label titleLabel = new Label();
    private final ImageView stepIllustration = new ImageView();
    private final Label bodyLabel = new Label();
    private final Button btnCloseCorner = new Button("✕");
    private final Button btnPrev = new Button();
    private final Button btnNext = new Button();
    private final ScrollPane bodyScroll = new ScrollPane();

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
        bodyLabel.maxWidthProperty().bind(
                Bindings.createDoubleBinding(
                        () -> {
                            double w = bodyScroll.getWidth();
                            return w > 40 ? w - 28 : 280;
                        },
                        bodyScroll.widthProperty()));
        stepIllustration.getStyleClass().add("tutorial-step-image");
        stepIllustration.setPreserveRatio(true);
        stepIllustration.setSmooth(true);
        stepIllustration.setVisible(false);
        stepIllustration.setManaged(false);
        VBox bodyColumn = new VBox(10, stepIllustration, bodyLabel);
        bodyColumn.setFillWidth(true);
        bodyColumn.maxWidthProperty().bind(
                Bindings.createDoubleBinding(
                        () -> {
                            double w = bodyScroll.getWidth();
                            return w > 40 ? w - 8 : 280;
                        },
                        bodyScroll.widthProperty()));
        bodyScroll.setContent(bodyColumn);
        bodyScroll.setFitToWidth(true);
        bodyScroll.setPrefViewportHeight(160);
        bodyScroll.setMaxHeight(240);
        bodyScroll.getStyleClass().add("tutorial-card-scroll");
        bodyScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        btnCloseCorner.getStyleClass().addAll("tutorial-card-close", "elite-nav-button");
        btnCloseCorner.setOnAction(e -> stop());
        HBox closeRow = new HBox();
        closeRow.setAlignment(Pos.CENTER_RIGHT);
        Region closeRowSpacer = new Region();
        HBox.setHgrow(closeRowSpacer, Priority.ALWAYS);
        closeRow.getChildren().addAll(closeRowSpacer, btnCloseCorner);
        titleLabel.maxWidthProperty().bind(
                Bindings.createDoubleBinding(
                        () -> {
                            double w = card.getWidth();
                            return w > 48 ? w - 52 : 260;
                        },
                        card.widthProperty()));
        VBox titleBlock = new VBox(8, closeRow, titleLabel);

        btnPrev.getStyleClass().add("elite-nav-button");
        btnNext.getStyleClass().addAll("elite-nav-button", "tutorial-card-primary");
        btnPrev.setOnAction(e -> goRelative(-1));
        btnNext.setOnAction(e -> goRelative(1));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox nav = new HBox(10, btnPrev, spacer, btnNext);
        nav.setAlignment(Pos.CENTER_LEFT);
        nav.setPadding(new Insets(4, 0, 0, 0));

        card.getChildren().addAll(titleBlock, bodyScroll, nav);
        card.getStyleClass().add("tutorial-card");
        card.setPadding(new Insets(14, 16, 14, 16));
        applyCardWidthConstraints(false);
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
        applyCardWidthConstraints(false);
    }

    private void applyCardWidthConstraints(boolean wide) {
        tutorialCardWide = wide;
        tutorialCardStep1Wide = false;
        if (wide) {
            double hw = host.getWidth();
            double adaptiveMin = hw > 0
                    ? Math.min(CARD_MIN_WIDTH_WIDE, Math.max(360, hw - 28))
                    : CARD_MIN_WIDTH_WIDE;
            card.setMinWidth(adaptiveMin);
            card.setMaxWidth(CARD_MAX_WIDTH_WIDE);
        } else {
            card.setMinWidth(CARD_MIN_WIDTH);
            card.setMaxWidth(CARD_MAX_WIDTH);
        }
    }

    /** Étape 1 sans image : carte plus large pour le long texte du stacking. */
    private void applyCardWidthConstraintsStep1() {
        tutorialCardWide = false;
        tutorialCardStep1Wide = true;
        card.setMinWidth(CARD_MIN_WIDTH_STEP1);
        card.setMaxWidth(CARD_MAX_WIDTH_STEP1);
    }

    public void refreshStepTextIfVisible() {
        if (!isShowing() || steps.isEmpty()) {
            return;
        }
        refreshLocalizedChrome();
        applyStepContent(steps.get(index));
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
        applyStepContent(step);
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

    private void applyStepContent(TutorialStep step) {
        titleLabel.setText(localizationService.getString(step.titleKey()));
        bodyLabel.setText(localizationService.getString(step.bodyKey()));
        String path = step.illustrationResourcePath();
        if (path != null && !path.isBlank()) {
            var url = ModuleTutorialOverlay.class.getResource(path.startsWith("/") ? path : "/" + path);
            if (url != null) {
                applyCardWidthConstraints(true);
                stepIllustration.setImage(new Image(url.toExternalForm(), true));
                double hostCap = host.getWidth() > 0 ? host.getWidth() - 20 : CARD_MAX_WIDTH_WIDE;
                double cardCap = Math.min(CARD_MAX_WIDTH_WIDE, hostCap);
                double imgW = Math.max(480, cardCap - 20);
                stepIllustration.setFitWidth(imgW);
                stepIllustration.setFitHeight(0);
                stepIllustration.setVisible(true);
                stepIllustration.setManaged(true);
                bodyScroll.setPrefViewportHeight(360);
                bodyScroll.setMaxHeight(680);
                return;
            }
        }
        if (index == 0) {
            applyCardWidthConstraintsStep1();
        } else {
            applyCardWidthConstraints(false);
        }
        stepIllustration.setImage(null);
        stepIllustration.setVisible(false);
        stepIllustration.setManaged(false);
        if (index == 0) {
            bodyScroll.setPrefViewportHeight(260);
            bodyScroll.setMaxHeight(400);
        } else {
            bodyScroll.setPrefViewportHeight(160);
            bodyScroll.setMaxHeight(240);
        }
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
        if (tutorialCardWide) {
            double adaptiveMin = Math.min(CARD_MIN_WIDTH_WIDE, Math.max(360, hostW - 28));
            card.setMinWidth(adaptiveMin);
        } else if (tutorialCardStep1Wide) {
            double adaptiveMin = hostW > 0
                    ? Math.min(CARD_MIN_WIDTH_STEP1, Math.max(420, hostW - 28))
                    : CARD_MIN_WIDTH_STEP1;
            card.setMinWidth(adaptiveMin);
        }
        double maxCard = tutorialCardWide
                ? CARD_MAX_WIDTH_WIDE
                : (tutorialCardStep1Wide ? CARD_MAX_WIDTH_STEP1 : CARD_MAX_WIDTH);
        double minCard = tutorialCardWide
                ? CARD_MIN_WIDTH_WIDE
                : (tutorialCardStep1Wide ? CARD_MIN_WIDTH_STEP1 : CARD_MIN_WIDTH);
        double cap = Math.min(maxCard, Math.max(200, hostW - 16));
        double minUse = Math.min(minCard, cap);
        double cw = tutorialCardWide
                ? cap
                : (tutorialCardStep1Wide
                        ? Math.min(maxCard, cap)
                        : Math.min(cap, Math.max(minUse, card.prefWidth(-1))));
        double ch = card.prefHeight(cw);
        if (tutorialCardWide && stepIllustration.isVisible() && stepIllustration.getImage() != null) {
            stepIllustration.setFitWidth(Math.max(420, cw - 16));
        }
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
