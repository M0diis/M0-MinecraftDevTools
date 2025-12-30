package me.m0dii.nbteditor.screens.configurable;

import me.m0dii.nbteditor.multiversion.TextInst;
import net.minecraft.text.Text;

public interface ConfigPathNamed extends ConfigPath {
    Text getName();

    Text getNamePrefix();

    void setNamePrefix(Text prefix);

    default Text getFullName() {
        Text name = getName();
        Text prefix = getNamePrefix();
        if (name == null) {
            return prefix == null ? null : prefix.copy();
        }
        if (prefix == null) {
            return name.copy();
        }
        return TextInst.copy(prefix).append(TextInst.copy(name));
    }
}
