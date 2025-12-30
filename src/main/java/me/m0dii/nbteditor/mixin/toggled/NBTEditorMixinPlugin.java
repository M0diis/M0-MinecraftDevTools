package me.m0dii.nbteditor.mixin.toggled;

import me.m0dii.nbteditor.misc.BasicMixinPlugin;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

import java.util.List;

public class NBTEditorMixinPlugin extends BasicMixinPlugin {

    @Override
    public void addMixins(List<String> output) {
        output.add("toggled." + ServerPlayNetworkHandlerMixin.class.getSimpleName());
        output.add("toggled." + ArmorSlotMixin.class.getSimpleName());
        output.add("toggled." + EnchantmentMixin.class.getSimpleName());

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
            return;
        }

        output.add("toggled." + DrawContextMixin.class.getSimpleName());
        output.add("toggled." + ItemStackMixin.class.getSimpleName());
        output.add("toggled." + RegistryEntryReferenceMixin.class.getSimpleName());
        output.add("toggled." + Registry1Mixin.class.getSimpleName());
        output.add("toggled." + TooltipMixin.class.getSimpleName());
        output.add("toggled." + GameRendererMixin_1_21_2.class.getSimpleName());
        output.add("toggled." + ItemModelManagerMixin.class.getSimpleName());
        output.add("toggled." + ItemRenderStateLayerRenderStateMixin.class.getSimpleName());
    }

}
