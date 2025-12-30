package me.m0dii.nbteditor.screens.configurable;

import net.minecraft.text.Text;

public class ConfigCategory extends ConfigGroupingVertical<String, ConfigCategory> {

    public ConfigCategory(Text name) {
        super(name, ConfigCategory::new);
    }

    public ConfigCategory() {
        this(null);
    }

}
