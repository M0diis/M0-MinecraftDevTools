package me.m0dii.nbteditor.multiversion;

import com.google.gson.JsonElement;
import me.m0dii.nbteditor.util.TextUtil;
import net.minecraft.text.Text;

public class TextInst {

    private TextInst() {
    }

    public static Text of(String msg) {
        return Text.of(msg);
    }

    public static EditableText literal(String msg) {
        return new EditableText(Text.literal(msg));
    }

    public static EditableText translatable(String key, Object... args) {
        return new EditableText(Text.stringifiedTranslatable(key, args));
    }

    public static EditableText copy(Text text) {
        return new EditableText(text.copy());
    }

    public static EditableText bracketed(Text text) {
        return translatable("chat.square_brackets", text);
    }

    /**
     * <strong>CONSIDER USING {@link TextUtil#fromJsonSafely(String)}</strong>
     */
    public static Text fromJson(String json) {
        return Text.Serialization.fromJson(json, DynamicRegistryManagerHolder.get());
    }

    public static String toJsonString(Text text) {
        return Text.Serialization.toJsonString(text, DynamicRegistryManagerHolder.get());
    }

    public static JsonElement toJsonTree(Text text) {
        return Text.Serialization.toJson(text, DynamicRegistryManagerHolder.get());
    }

}
