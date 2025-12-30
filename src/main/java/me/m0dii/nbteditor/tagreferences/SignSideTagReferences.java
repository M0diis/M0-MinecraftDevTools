package me.m0dii.nbteditor.tagreferences;

import me.m0dii.nbteditor.tagreferences.general.NBTTagReference;
import me.m0dii.nbteditor.tagreferences.general.TagReference;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

import java.util.List;

public class SignSideTagReferences {

    private SignSideTagReferences() {
    }

    public static final TagReference<Boolean, NbtCompound> GLOWING = new NBTTagReference<>(Boolean.class, "has_glowing_text");
    public static final TagReference<String, NbtCompound> COLOR = new NBTTagReference<>(String.class, "color");
    public static final TagReference<List<Text>, NbtCompound> TEXT = TagReference.forLists(Text.class, new NBTTagReference<>(Text[].class, "messages"));

}
