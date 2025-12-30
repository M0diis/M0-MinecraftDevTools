package me.m0dii.nbteditor.misc;

public interface ResetableDataTracker {
    default void reset() {
        throw new RuntimeException("Missing implementation for ResetableDataTracker#reset");
    }
}
