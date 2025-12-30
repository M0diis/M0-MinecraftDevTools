package me.m0dii.nbteditor.screens.factories;

import me.m0dii.nbteditor.localnbt.LocalBlock;
import me.m0dii.nbteditor.localnbt.LocalItem;
import me.m0dii.nbteditor.localnbt.LocalNBT;
import me.m0dii.nbteditor.multiversion.DrawableHelper;
import me.m0dii.nbteditor.multiversion.TextInst;
import me.m0dii.nbteditor.nbtreferences.NBTReference;
import me.m0dii.nbteditor.screens.LocalEditorScreen;
import me.m0dii.nbteditor.screens.configurable.ConfigCategory;
import me.m0dii.nbteditor.screens.configurable.ConfigItem;
import me.m0dii.nbteditor.screens.configurable.ConfigPanel;
import me.m0dii.nbteditor.screens.configurable.ConfigValueDropdown;
import me.m0dii.nbteditor.tagreferences.ItemTagReferences;
import me.m0dii.nbteditor.util.BlockStateProperties;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;

import java.util.*;

public class BlockStatesScreen<L extends LocalNBT> extends LocalEditorScreen<L> {

    private final ConfigCategory blockStates;
    private final boolean hasBlockStates;
    private ConfigPanel panel;

    public BlockStatesScreen(NBTReference<L> ref) {
        super(TextInst.of("Block States"), ref);

        BlockStateProperties defaultState;
        BlockStateProperties state;
        Set<String> unset;
        switch (localNBT) {
            case LocalItem item -> {
                defaultState = new BlockStateProperties(((BlockItem) item.getItemType()).getBlock().getDefaultState());
                state = defaultState.copy();
                unset = state.setValuesMap(ItemTagReferences.BLOCK_STATE.get(item.getEditableItem()));
            }
            case LocalBlock block -> {
                defaultState = new BlockStateProperties(block.getBlock().getDefaultState());
                state = block.getState();
                unset = new HashSet<>();
            }
            default -> {
                throw new IllegalStateException("BlockStatesScreen doesn't support " + localNBT.getClass().getName());
            }
        }

        this.hasBlockStates = !defaultState.getProperties().isEmpty();
        this.blockStates = new ConfigCategory(this.hasBlockStates ? TextInst.translatable("nbteditor.block_states") : null);

        for (String property : defaultState.getProperties()) {
            String value = (unset.contains(property) ? "unset" : state.getValue(property));

            List<String> options = new ArrayList<>(defaultState.getOptions(property));
            if (localNBT instanceof LocalItem) {
                options.add(0, "unset");
            }

            blockStates.setConfigurable(property, new ConfigItem<>(TextInst.literal(property),
                    ConfigValueDropdown.forList(value, defaultState.getValue(property), options)
                            .addValueListener(dropdown -> {
                                String newValue = dropdown.getValidValue();

                                if (newValue.equals("unset")) {
                                    unset.add(property);
                                } else {
                                    unset.remove(property);
                                    state.setValue(property, newValue);
                                }

                                if (localNBT instanceof LocalItem item) {
                                    Map<String, String> blockStatesMap = state.getValuesMap();
                                    blockStatesMap.keySet().removeAll(unset);
                                    ItemTagReferences.BLOCK_STATE.set(item.getEditableItem(), blockStatesMap);
                                }

                                checkSave();
                            })));
        }
    }

    @Override
    protected void initEditor() {
        ConfigPanel newPanel = addDrawableChild(new ConfigPanel(16, 64, width - 32, height - 80, blockStates));
        if (panel != null) {
            newPanel.setScroll(panel.getScroll());
        }
        panel = newPanel;
    }

    @Override
    public void renderEditor(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (!hasBlockStates) {
            DrawableHelper.drawTextWithShadow(matrices, textRenderer, TextInst.translatable("nbteditor.block_states.none"), 16, 64, -1);
        }
    }

}
