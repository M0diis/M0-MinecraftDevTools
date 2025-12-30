package me.m0dii.nbteditor.screens.configurable;

import net.minecraft.text.Text;

import java.util.function.BiFunction;

public class ConfigHiddenDataNamed<S extends ConfigPathNamed, D> extends ConfigHiddenData<S, D> implements ConfigPathNamed {

    public ConfigHiddenDataNamed(S visible, D data, BiFunction<D, Boolean, D> onClone) {
        super(visible, data, onClone);
    }

    @Override
    public Text getName() {
        return visible.getName();
    }

    @Override
    public Text getNamePrefix() {
        return visible.getNamePrefix();
    }

    @Override
    public void setNamePrefix(Text prefix) {
        visible.setNamePrefix(prefix);
    }

    @Override
    public Text getFullName() {
        return visible.getFullName();
    }

    @SuppressWarnings("unchecked")
    @Override
    public ConfigHiddenDataNamed<S, D> clone(boolean defaults) {
        return new ConfigHiddenDataNamed<>((S) visible.clone(defaults), onClone.apply(data, defaults), onClone);
    }

}
