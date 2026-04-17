package be.mirooz.elitedangerous.dashboard.view.common.managers;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.IdentityHashMap;
import java.util.Map;

public final class NotificationStackManager {

    private static final NotificationStackManager INSTANCE = new NotificationStackManager();
    private static final String STACK_STYLE_CLASS = "top-right-notification-stack";
    private final Map<StackPane, VBox> stacksByContainer = new IdentityHashMap<>();

    private NotificationStackManager() {
    }

    public static NotificationStackManager getInstance() {
        return INSTANCE;
    }

    public VBox getOrCreateStack(StackPane container) {
        VBox existing = stacksByContainer.get(container);
        if (existing != null) {
            return existing;
        }

        VBox stack = new VBox(12);
        stack.getStyleClass().add(STACK_STYLE_CLASS);
        stack.setAlignment(Pos.TOP_RIGHT);
        stack.setPickOnBounds(false);

        StackPane.setAlignment(stack, Pos.TOP_RIGHT);
        StackPane.setMargin(stack, new Insets(60, 20, 0, 0));

        container.getChildren().add(stack);
        stacksByContainer.put(container, stack);
        return stack;
    }
}
