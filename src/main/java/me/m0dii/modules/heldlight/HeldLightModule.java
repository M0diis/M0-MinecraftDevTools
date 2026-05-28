package me.m0dii.modules.heldlight;

import me.m0dii.modules.Module;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class HeldLightModule extends Module {
    public static final HeldLightModule INSTANCE = new HeldLightModule();

    private HeldLightModule() {
        super("held_light", "Held Light", false);
    }

    @Override
    public void register() {
        HeldLightConfigDataHandler.load();
    }

    public static boolean isLightSource(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        HeldLightConfigDataHandler.Config cfg = HeldLightConfigDataHandler.get();
        String id = Registries.ITEM.getId(stack.getItem()).toString();
        for (String itemId : cfg.itemWhitelist) {
            if (itemId.equals(id)) {
                return true;
            }
        }
        for (String tagId : cfg.tagWhitelist) {
            Identifier parsed = Identifier.tryParse(tagId);
            if (parsed == null) {
                continue;
            }
            TagKey<net.minecraft.item.Item> tag = TagKey.of(RegistryKeys.ITEM, parsed);
            if (stack.isIn(tag)) {
                return true;
            }
        }
        return false;
    }
}

