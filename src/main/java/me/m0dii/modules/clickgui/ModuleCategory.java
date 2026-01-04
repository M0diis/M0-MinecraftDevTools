package me.m0dii.modules.clickgui;

import lombok.Getter;
import me.m0dii.modules.Module;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a category of modules in the ClickGUI.
 */
@Getter
public class ModuleCategory {
    private final String name;
    private final List<Module> modules;

    public ModuleCategory(String name) {
        this.name = name;
        this.modules = new ArrayList<>();
    }

    public void addModule(Module module) {
        this.modules.add(module);
    }
}

