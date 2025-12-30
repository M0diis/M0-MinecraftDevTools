package me.m0dii.nbteditor.screens.configurable;

import me.m0dii.nbteditor.screens.Tickable;

public interface ConfigPath extends Configurable<ConfigPath>, Tickable {
    ConfigPath addValueListener(ConfigValueListener<ConfigValue<?, ?>> listener);
}
