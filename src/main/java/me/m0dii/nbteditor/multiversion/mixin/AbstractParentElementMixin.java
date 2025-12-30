package me.m0dii.nbteditor.multiversion.mixin;

import me.m0dii.nbteditor.multiversion.MVElement;
import me.m0dii.nbteditor.multiversion.OldEventBehavior;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.client.gui.AbstractParentElement;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractParentElement.class)
public class AbstractParentElementMixin {

    @Inject(method = "setFocused", at = @At("RETURN"))
    private void setFocused(Element element, CallbackInfo info) {
        boolean oldEvents = MiscUtil.client.currentScreen instanceof OldEventBehavior;
        for (Element child : ((AbstractParentElement) (Object) this).children()) {
            if (child instanceof MVElement multiChild) {
                multiChild.setMultiFocused(child == element);
            }

            if (oldEvents && child instanceof TextFieldWidget textChild) {
                textChild.setFocused(child == element);
            }
        }
    }

}
