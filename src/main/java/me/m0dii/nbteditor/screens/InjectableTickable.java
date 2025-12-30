package me.m0dii.nbteditor.screens;

public interface InjectableTickable extends Tickable {
    @Override
    default void tick() {
        throw new RuntimeException("Missing implementation for InjectableTickable#tick" + this.getClass().getName());
    }
}
