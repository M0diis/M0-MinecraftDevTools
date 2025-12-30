package me.m0dii.nbteditor.mixin;

import me.m0dii.nbteditor.multiversion.IdentifierInst;
import me.m0dii.nbteditor.screens.Tickable;
import me.m0dii.nbteditor.screens.widgets.NamedTextFieldWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(TextFieldWidget.class)
public abstract class TextFieldWidgetMixin implements Tickable {
    private static final Identifier TEXT_FIELD_INVALID = IdentifierInst.of("m0-dev-tools", "widget/text_field_invalid");

    @ModifyArg(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIII)V", ordinal = 0))
    @Group(name = "renderButton", min = 1)
    private Identifier drawGuiTexture2(Identifier texture) {
        TextFieldWidget source = (TextFieldWidget) (Object) this;
        if (source instanceof NamedTextFieldWidget named && !named.isValid()) {
            return TEXT_FIELD_INVALID;
        }
        return texture;
    }

    @Override
    public void tick() {
    }
}
