package me.m0dii.nbteditor.screens;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.Getter;
import lombok.Setter;
import me.m0dii.nbteditor.localnbt.LocalItem;
import me.m0dii.nbteditor.localnbt.LocalNBT;
import me.m0dii.nbteditor.misc.MixinLink;
import me.m0dii.nbteditor.multiversion.DrawableHelper;
import me.m0dii.nbteditor.multiversion.IdentifierInst;
import me.m0dii.nbteditor.multiversion.MVTooltip;
import me.m0dii.nbteditor.multiversion.nbt.NBTManagers;
import me.m0dii.nbteditor.screens.nbtfolder.NBTFolder;
import me.m0dii.nbteditor.screens.widgets.List2D;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.nbt.AbstractNbtList;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

public class NBTValue extends List2D.List2DValue {

    private static final Identifier BACK = IdentifierInst.of("m0-dev-tools", "textures/nbt/back.png");
    private static final Identifier BYTE = IdentifierInst.of("m0-dev-tools", "textures/nbt/byte.png");
    private static final Identifier SHORT = IdentifierInst.of("m0-dev-tools", "textures/nbt/short.png");
    private static final Identifier INT = IdentifierInst.of("m0-dev-tools", "textures/nbt/int.png");
    private static final Identifier LONG = IdentifierInst.of("m0-dev-tools", "textures/nbt/long.png");
    private static final Identifier FLOAT = IdentifierInst.of("m0-dev-tools", "textures/nbt/float.png");
    private static final Identifier DOUBLE = IdentifierInst.of("m0-dev-tools", "textures/nbt/double.png");
    private static final Identifier NUMBER = IdentifierInst.of("m0-dev-tools", "textures/nbt/number.png");
    private static final Identifier STRING = IdentifierInst.of("m0-dev-tools", "textures/nbt/string.png");
    private static final Identifier LIST = IdentifierInst.of("m0-dev-tools", "textures/nbt/list.png");
    private static final Identifier BYTE_ARRAY = IdentifierInst.of("m0-dev-tools", "textures/nbt/byte_array.png");
    private static final Identifier INT_ARRAY = IdentifierInst.of("m0-dev-tools", "textures/nbt/int_array.png");
    private static final Identifier LONG_ARRAY = IdentifierInst.of("m0-dev-tools", "textures/nbt/long_array.png");
    private static final Identifier COMPOUND = IdentifierInst.of("m0-dev-tools", "textures/nbt/compound.png");

    private final NBTEditorScreen<?> screen;
    @Getter
    private final String key;
    private NbtElement value;
    private final AbstractNbtList<?> parentList;

    private boolean selected;
    /**
     * -- GETTER --
     *
     * @return Returns if this value has been manually set as unsafe; doesn't take into account list types
     */
    @Setter
    @Getter
    private boolean unsafe;
    @Getter
    @Setter
    private boolean invalidComponent;

    public NBTValue(NBTEditorScreen<?> screen, String key, NbtElement value, AbstractNbtList<?> parentList) {
        this.screen = screen;
        this.key = key;
        this.value = value;
        this.parentList = parentList;
    }

    public NBTValue(NBTEditorScreen<?> screen, String key, NbtElement value) {
        this(screen, key, value, null);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        Identifier icon = null;
        if (key == null) {
            icon = BACK;
        } else if (value.getType() == NbtElement.BYTE_TYPE) {
            icon = BYTE;
        } else if (value.getType() == NbtElement.SHORT_TYPE) {
            icon = SHORT;
        } else if (value.getType() == NbtElement.INT_TYPE) {
            icon = INT;
        } else if (value.getType() == NbtElement.LONG_TYPE) {
            icon = LONG;
        } else if (value.getType() == NbtElement.FLOAT_TYPE) {
            icon = FLOAT;
        } else if (value.getType() == NbtElement.DOUBLE_TYPE) {
            icon = DOUBLE;
        } else if (value.getType() == NbtElement.NUMBER_TYPE) {
            icon = NUMBER;
        } else if (value.getType() == NbtElement.STRING_TYPE) {
            icon = STRING;
        } else if (value.getType() == NbtElement.LIST_TYPE) {
            icon = LIST;
        } else if (value.getType() == NbtElement.BYTE_ARRAY_TYPE) {
            icon = BYTE_ARRAY;
        } else if (value.getType() == NbtElement.INT_ARRAY_TYPE) {
            icon = INT_ARRAY;
        } else if (value.getType() == NbtElement.LONG_ARRAY_TYPE) {
            icon = LONG_ARRAY;
        } else if (value.getType() == NbtElement.COMPOUND_TYPE) {
            icon = COMPOUND;
        }

        if (icon != null) {
            DrawableHelper.drawTexture(matrices, icon, 0, 0, 0, 0, 32, 32, 32, 32);
        }

        int color = -1;
        String tooltip = null;
        if (unsafe && selected || parentList != null && parentList.getHeldType() != value.getType()) {
            color = 0xFFFFAA33;
            tooltip = "nbteditor.nbt.marker.unsafe";
        } else if (invalidComponent) {
            color = 0xFF550000;
            tooltip = "nbteditor.nbt.marker.invalid_component";
        } else if (selected) {
            color = 0xFFDF4949;
        } else if (isHovering(mouseX, mouseY)) {
            color = 0xFF257789;
        }
        if (color != -1) {
            DrawableHelper.fill(matrices, -2, -2, 36, 0, color);
            DrawableHelper.fill(matrices, -2, -2, 0, 36, color);
            DrawableHelper.fill(matrices, -2, 34, 36, 36, color);
            DrawableHelper.fill(matrices, 34, -2, 36, 36, color);
        }
        if (tooltip != null && isHovering(mouseX, mouseY)) {
            new MVTooltip(tooltip).render(matrices, mouseX, mouseY);
        }

        if (key == null) {
            return;
        }

        matrices.push();
        matrices.scale((float) ConfigScreen.getKeyTextSize(), (float) ConfigScreen.getKeyTextSize(), 0);
        double scale = 1 / ConfigScreen.getKeyTextSize();
        MiscUtil.drawWrappingString(matrices, textRenderer, key, (int) (16 * scale), (int) (24 * scale), (int) (32 * scale), -1, true, true);
        matrices.pop();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovering((int) mouseX, (int) mouseY)) {
            if (key == null) {
                screen.selectNbt(null, true);
                return true;
            }

            NBTFolder<?> folder = NBTFolder.get(value);
            screen.selectNbt(this, selected && folder != null && !folder.hasEmptyKey());
            selected = !selected;
            return selected;
        }

        selected = false;
        return false;
    }

    private boolean isHovering(int mouseX, int mouseY) {
        return isInsideList() && mouseX >= 0 && mouseY >= 0 && mouseX <= 32 && mouseY <= 32;
    }

    public void valueChanged(String str, Consumer<NbtElement> onChange) {
        try {
            value = MixinLink.parseSpecialElement(new StringReader(str));
            onChange.accept(value);
        } catch (CommandSyntaxException ignored) {
        }
    }

    public String getValueText(boolean json) {
        if (json && value instanceof NbtByte valueByte) {
            if (valueByte.byteValue() == 0) {
                return String.valueOf(false);
            }
            if (valueByte.byteValue() == 1) {
                return String.valueOf(true);
            }
        }
        return value.toString();
    }

    public void updateInvalidComponent(LocalNBT localNBT, String component) {
        if (localNBT instanceof LocalItem localItem) {
            NbtCompound nbtOutput = NBTManagers.ITEM.getNbt(localItem.getReadableItem());
            if (component == null) {
                component = this.key;
            }
            this.invalidComponent = (nbtOutput == null || !nbtOutput.contains(MiscUtil.addNamespace(component)));
        }
    }

}
