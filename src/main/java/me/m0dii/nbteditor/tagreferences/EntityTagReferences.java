package me.m0dii.nbteditor.tagreferences;

import me.m0dii.nbteditor.localnbt.LocalEntity;
import me.m0dii.nbteditor.tagreferences.general.NBTTagReference;
import me.m0dii.nbteditor.tagreferences.general.TagReference;
import me.m0dii.nbteditor.tagreferences.specific.AttributesNBTTagReference;
import me.m0dii.nbteditor.tagreferences.specific.data.AttributeData;

import java.util.ArrayList;
import java.util.List;

public class EntityTagReferences {

    public static final TagReference<List<AttributeData>, LocalEntity> ATTRIBUTES =
            TagReference.forLocalNBT(ArrayList::new, new AttributesNBTTagReference(AttributesNBTTagReference.NBTLayout.ENTITY_NEW));

    public static final TagReference<Boolean, LocalEntity> CUSTOM_NAME_VISIBLE =
            TagReference.forLocalNBT(() -> false, new NBTTagReference<>(Boolean.class, "CustomNameVisible"));

}
