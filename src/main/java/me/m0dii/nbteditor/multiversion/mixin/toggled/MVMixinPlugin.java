package me.m0dii.nbteditor.multiversion.mixin.toggled;

import me.m0dii.nbteditor.misc.BasicMixinPlugin;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

import java.util.List;

public class MVMixinPlugin extends BasicMixinPlugin {

    @Override
    public void addMixins(List<String> output) {

        output.add("toggled." + ItemStackMixin.class.getSimpleName());

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
            return;
        }

        output.add("toggled." + ScreenMixin.class.getSimpleName());
        output.add("toggled." + BookScreenContentsMixin.class.getSimpleName());
    }

}
