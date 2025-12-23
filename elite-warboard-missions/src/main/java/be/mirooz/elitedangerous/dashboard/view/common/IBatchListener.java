package be.mirooz.elitedangerous.dashboard.view.common;

public interface IBatchListener {
    default void onBatchStart() {}
    default void onBatchEnd() {}
}
