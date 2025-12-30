package me.m0dii.nbteditor.screens.factories;

import me.m0dii.nbteditor.localnbt.LocalEntity;
import me.m0dii.nbteditor.localnbt.LocalItem;
import me.m0dii.nbteditor.localnbt.LocalNBT;
import me.m0dii.nbteditor.multiversion.MVComponentType;
import me.m0dii.nbteditor.multiversion.MVMisc;
import me.m0dii.nbteditor.multiversion.TextInst;
import me.m0dii.nbteditor.nbtreferences.NBTReference;
import me.m0dii.nbteditor.nbtreferences.itemreferences.ItemReference;
import me.m0dii.nbteditor.screens.LocalEditorScreen;
import me.m0dii.nbteditor.screens.widgets.FormattedTextFieldWidget;
import me.m0dii.nbteditor.screens.widgets.ImageToLoreWidget;
import me.m0dii.nbteditor.tagreferences.EntityTagReferences;
import me.m0dii.nbteditor.tagreferences.ItemTagReferences;
import me.m0dii.nbteditor.util.MiscUtil;
import me.m0dii.nbteditor.util.StyleUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DisplayScreen<L extends LocalNBT> extends LocalEditorScreen<L> {

    private FormattedTextFieldWidget nameFormatted;
    private FormattedTextFieldWidget lore;
    private boolean itemNameType;

    public DisplayScreen(NBTReference<L> ref) {
        super(TextInst.of("Display"), ref);
    }

    @Override
    protected void initEditor() {
        nameFormatted = FormattedTextFieldWidget.create(nameFormatted, 16, 64, width - 32, 24 + textRenderer.fontHeight * 3,
                itemNameType ? MiscUtil.getBaseItemNameSafely(((LocalItem) localNBT).getEditableItem()) : localNBT.getName(),
                false, StyleUtil.getBaseNameStyle(localNBT, itemNameType), text -> {
                    if (itemNameType) {
                        ((LocalItem) localNBT).getEditableItem().set(MVComponentType.ITEM_NAME, text);
                    } else {
                        localNBT.setName(text);
                    }
                    name.setText(localNBT.getName().getString());
                    checkSave();
                }).setOverscroll(false).setShadow(localNBT instanceof LocalItem);

        int nextY = 64 + 24 + textRenderer.fontHeight * 3 + 4;

        if (localNBT instanceof LocalItem item) {
            lore = FormattedTextFieldWidget.create(lore, 16, nextY, width - 32, height - 16 - 20 - 4 - nextY,
                    ItemTagReferences.LORE.get(item.getEditableItem()), StyleUtil.BASE_LORE_STYLE, lines -> {
                        if (lines.size() == 1 && lines.getFirst().getString().isEmpty()) {
                            ItemTagReferences.LORE.set(item.getEditableItem(), new ArrayList<>());
                        } else {
                            ItemTagReferences.LORE.set(item.getEditableItem(), lines);
                        }
                        checkSave();
                    });
            addSelectableChild(nameFormatted);
            addSelectableChild(lore);
            addDrawableChild(MVMisc.newButton(16, height - 16 - 20, 100, 20, TextInst.translatable("nbteditor.hide_flags"),
                    btn -> closeSafely(() -> client.setScreen(new HideFlagsScreen((ItemReference) ref)))));
            addDrawableChild(MVMisc.newButton(124, height - 16 - 20, 150, 20,
                    TextInst.translatable("nbteditor.display.name_type." + (itemNameType ? "item" : "custom")), btn -> {
                        itemNameType = !itemNameType;
                        btn.setMessage(TextInst.translatable("nbteditor.display.name_type." + (itemNameType ? "item" : "custom")));
                        nameFormatted = null;
                        clearChildren();
                        init();
                    }));
            addDrawable(lore);
        } else {
            addSelectableChild(nameFormatted);
        }

        if (localNBT instanceof LocalEntity entity) {
            addDrawableChild(MVMisc.newButton(16, nextY, 150, 20,
                    TextInst.translatable("nbteditor.display.custom_name_visible." +
                            (EntityTagReferences.CUSTOM_NAME_VISIBLE.get(entity) ? "enabled" : "disabled")), btn -> {
                        boolean customNameVisible = !EntityTagReferences.CUSTOM_NAME_VISIBLE.get(entity);
                        EntityTagReferences.CUSTOM_NAME_VISIBLE.set(entity, customNameVisible);
                        btn.setMessage(TextInst.translatable("nbteditor.display.custom_name_visible." + (customNameVisible ? "enabled" : "disabled")));
                        checkSave();
                    }));
        }
    }

    @Override
    protected void renderEditor(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        matrices.push();
        matrices.translate(0.0, 0.0, 1.0);
        nameFormatted.render(matrices, mouseX, mouseY, delta);
        matrices.pop();
    }

    @Override
    public void onFilesDropped(List<Path> paths) {
        if (!(localNBT instanceof LocalItem)) {
            return;
        }

        List<Text> lines = new ArrayList<>();
        lines.add(lore.getText());

        ImageToLoreWidget.openImportFiles(paths, (file, imgLines) -> lines.addAll(imgLines), () -> {
            if (lines.size() > 1) {
                lore.setText(lines);
            }
        });
    }

}
