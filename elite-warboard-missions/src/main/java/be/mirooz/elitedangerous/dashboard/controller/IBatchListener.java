package be.mirooz.elitedangerous.dashboard.controller;

public interface IBatchListener {
    default void onBatchStart() {}
    default void onBatchEnd() {}
}
