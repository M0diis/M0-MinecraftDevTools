package me.m0dii.modules.xray;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class XrayConfigScreen {
    private XrayConfigScreen() {
    }

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setShouldListSmoothScroll(true)
                .solidBackground()
                .setTitle(Text.literal("Xray Block Config"));

        ConfigEntryBuilder eb = builder.entryBuilder();
        ConfigCategory category = builder.getOrCreateCategory(Text.literal("Xray Blocks"));

        Set<String> blocks = new HashSet<>(XrayManager.getXrayBlocks());
        List<String> blockList = new ArrayList<>(blocks);

        category.addEntry(eb.startStrList(Text.literal("Blocks to show in Xray"), blockList)
                .setTooltip(Text.literal("Enter block IDs (e.g., minecraft:diamond_ore). Only these blocks will be visible in xray mode."))
                .setSaveConsumer(list -> {
                    blocks.clear();
                    blocks.addAll(list);
                })
                .setDefaultValue(List.of("minecraft:diamond_ore"))
                .build());

        builder.setSavingRunnable(() -> XrayManager.setXrayBlocks(blocks));

        return builder.build();
    }
}

