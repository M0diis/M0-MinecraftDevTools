package me.m0dii.nbteditor.screens;

import me.m0dii.M0DevTools;
import me.m0dii.nbteditor.localnbt.LocalBlock;
import me.m0dii.nbteditor.localnbt.LocalEntity;
import me.m0dii.nbteditor.localnbt.LocalItem;
import me.m0dii.nbteditor.localnbt.LocalNBT;
import me.m0dii.nbteditor.multiversion.*;
import me.m0dii.nbteditor.screens.widgets.ImageToLoreWidget;
import me.m0dii.nbteditor.screens.widgets.ImportPosWidget;
import me.m0dii.nbteditor.screens.widgets.NamedTextFieldWidget;
import me.m0dii.nbteditor.tagreferences.ItemTagReferences;
import me.m0dii.nbteditor.util.MiscUtil;
import me.m0dii.nbteditor.util.TextUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class ImportScreen extends OverlaySupportingScreen {

    private final List<Text> msg;
    private NamedTextFieldWidget dataVersion;

    public ImportScreen() {
        super(TextInst.of("Import"));
        msg = TextUtil.getLongTranslatableTextLines("nbteditor.nbt.import.desc");
    }

    public static void importFiles(List<Path> paths, Optional<Integer> defaultDataVersion) {
        List<Consumer<BlockPos>> posConsumers = new ArrayList<>();

        for (Path path : paths) {
            File file = path.toFile();
            if (!file.isFile()) {
                continue;
            }

            if (file.getName().endsWith(".nbt")) {
                try (FileInputStream in = new FileInputStream(file)) {
                    NbtCompound nbt = MiscUtil.readNBT(in);
                    if (defaultDataVersion.isEmpty() && !nbt.contains("DataVersion", NbtElement.NUMBER_TYPE)) {
                        MiscUtil.client.player.sendMessage(TextUtil.parseTranslatableFormatted("nbteditor.nbt.import.data_version.unknown", file.getName()), false);
                    }
                    if (nbt.getInt("DataVersion") > Version.getDataVersion()) {
                        MiscUtil.client.player.sendMessage(TextInst.translatable("nbteditor.nbt.import.data_version.new", file.getName()), false);
                    }
                    LocalNBT.deserialize(nbt, defaultDataVersion.orElse(Version.getDataVersion())).ifPresent(localNBT -> {
                        switch (localNBT) {
                            case LocalItem item -> item.receive();
                            case LocalBlock block -> posConsumers.add(block::place);
                            case LocalEntity entity ->
                                    posConsumers.add(pos -> entity.summon(MiscUtil.client.world.getRegistryKey(), Vec3d.ofCenter(pos)));
                            default -> {
                            }
                        }
                    });
                } catch (Exception e) {
                    M0DevTools.LOGGER.error("Error while importing a .nbt file", e);
                    MiscUtil.client.player.sendMessage(TextInst.literal(e.getClass().getName() + ": " + e.getMessage()).formatted(Formatting.RED), false);
                }
            }
        }

        if (!posConsumers.isEmpty()) {
            ImportPosWidget.openImportPos(MiscUtil.client.player.getBlockPos(),
                    pos -> posConsumers.forEach(consumer -> consumer.accept(pos)));
            return;
        }

        ImageToLoreWidget.openImportFiles(paths, (file, imgLore) -> {
            String name = file.getName();
            int nameDot = name.lastIndexOf('.');
            if (nameDot != -1) {
                name = name.substring(0, nameDot);
            }

            ItemStack painting = new ItemStack(Items.PAINTING);
            painting.manager$setCustomName(TextInst.literal(name).styled(style -> style.withItalic(false).withColor(Formatting.GOLD)));
            ItemTagReferences.LORE.set(painting, imgLore);
            MiscUtil.getWithMessage(painting);
        }, () -> {
        });
    }

    @Override
    protected void init() {
        super.init();
        dataVersion = addDrawableChild(
                new NamedTextFieldWidget(16, 64 + textRenderer.fontHeight * msg.size() + 16, 100, 16, dataVersion)
                        .name(TextInst.translatable("nbteditor.nbt.import.data_version"))
                        .tooltip(new MVTooltip("nbteditor.nbt.import.data_version.desc")));
        addDrawableChild(MVMisc.newButton(this.width - 116, this.height - 36, 100, 20, ScreenTexts.DONE, btn -> close()));
    }

    @Override
    protected void renderMain(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        dataVersion.setValid(dataVersion.getText().isEmpty() ||
                Version.getDataVersion(dataVersion.getText()).filter(value -> value <= Version.getDataVersion()).isPresent());

        super.renderBackground(matrices);
        super.renderMain(matrices, mouseX, mouseY, delta);

        for (int i = 0; i < msg.size(); i++) {
            DrawableHelper.drawText(matrices, textRenderer, msg.get(i), 16, 64 + textRenderer.fontHeight * i, -1, true);
        }
    }

    @Override
    public void onFilesDropped(List<Path> paths) {
        importFiles(paths, Version.getDataVersion(dataVersion.getText()).filter(value -> value <= Version.getDataVersion()));
    }

}
