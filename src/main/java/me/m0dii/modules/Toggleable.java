package me.m0dii.modules;

public interface Toggleable {
    boolean isEnabled();

    void setEnabled(boolean enabled);

    default void toggle() {
        setEnabled(!isEnabled());
    }
}
