package me.m0dii.nbteditor.multiversion;

import me.m0dii.nbteditor.util.Reflection;
import net.minecraft.client.gui.Element;

import java.lang.invoke.MethodType;
import java.util.function.Supplier;

public interface MVElementParent {
    Supplier<Reflection.MethodInvoker> Element_mouseScrolled =
            Reflection.getOptionalMethod(Element.class, "method_25401", MethodType.methodType(boolean.class, double.class, double.class, double.class));

    default boolean mouseScrolled(double mouseX, double mouseY, double xAmount, double yAmount) {
        return Element_mouseScrolled.get().invoke(this, mouseX, mouseY, yAmount);
    }
}
