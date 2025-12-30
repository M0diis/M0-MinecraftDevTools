package me.m0dii.nbteditor.screens.configurable;

@FunctionalInterface
public interface ConfigValueListener<V extends ConfigValue<?, ?>> {
    void onValueChanged(V source);
}
