package me.m0dii.nbteditor.screens.widgets;

import me.m0dii.nbteditor.multiversion.DrawableHelper;
import me.m0dii.nbteditor.multiversion.MVMisc;
import me.m0dii.nbteditor.multiversion.ScreenTexts;
import me.m0dii.nbteditor.multiversion.TextInst;
import me.m0dii.nbteditor.screens.OverlaySupportingScreen;
import me.m0dii.nbteditor.screens.WidgetScreen;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;
import java.util.function.Consumer;

public class ImportPosWidget extends GroupWidget implements InitializableOverlay<Screen> {

    private final BlockPos defaultPos;
    private final Consumer<Optional<BlockPos>> posConsumer;
    private final TextRenderer textRenderer;
    private int width;
    private int height;
    private NamedTextFieldWidget x;
    private NamedTextFieldWidget y;
    private NamedTextFieldWidget z;

    public ImportPosWidget(BlockPos defaultPos, Consumer<Optional<BlockPos>> posConsumer) {
        this.defaultPos = defaultPos;
        this.posConsumer = posConsumer;
        this.textRenderer = MiscUtil.client.textRenderer;
    }

    public static void openImportPos(BlockPos defaultPos, Consumer<BlockPos> posConsumer) {
        WidgetScreen.setOverlayOrScreen(new ImportPosWidget(defaultPos, optional -> {
            OverlaySupportingScreen.setOverlayStatic(null);
            optional.ifPresent(posConsumer);
        }), 200, true);
    }

    @Override
    public void init(Screen parent, int width, int height) {
        clearWidgets();

        this.width = width;
        this.height = height;

        boolean firstInit = (x == null);

        x = addWidget(new NamedTextFieldWidget(width / 2 - 102, height / 2 - 18, 65, 16, x)
                .name(TextInst.translatable("nbteditor.nbt.import.pos.x")));
        y = addWidget(new NamedTextFieldWidget(width / 2 - 33, height / 2 - 18, 65, 16, y)
                .name(TextInst.translatable("nbteditor.nbt.import.pos.y")));
        z = addWidget(new NamedTextFieldWidget(width / 2 + 36, height / 2 - 18, 66, 16, z)
                .name(TextInst.translatable("nbteditor.nbt.import.pos.z")));

        x.setTextPredicate(MiscUtil.intPredicate());
        y.setTextPredicate(MiscUtil.intPredicate());
        z.setTextPredicate(MiscUtil.intPredicate());

        if (firstInit) {
            x.setText("" + defaultPos.getX());
            y.setText("" + defaultPos.getY());
            z.setText("" + defaultPos.getZ());
        }

        addWidget(MVMisc.newButton(width / 2 - 102, height / 2 + 2, 100, 20, ScreenTexts.DONE, btn -> done()));
        addWidget(MVMisc.newButton(width / 2 + 2, height / 2 + 2, 100, 20, ScreenTexts.CANCEL, btn -> posConsumer.accept(Optional.empty())));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (!(MiscUtil.client.currentScreen instanceof WidgetScreen)) {
            DrawableHelper.fill(matrices, width / 2 - 102 - 16, height / 2 - 18 - 16, width / 2 + 102 + 16, height / 2 + 22 + 16, 0xC8101010);
        }
        super.render(matrices, mouseX, mouseY, delta);
        DrawableHelper.drawCenteredTextWithShadow(matrices, textRenderer, TextInst.translatable("nbteditor.nbt.import.pos"),
                width / 2, height / 2 - textRenderer.fontHeight - 22, -1);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            OverlaySupportingScreen.setOverlayStatic(null);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            done();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void done() {
        int xValue = MiscUtil.parseDefaultInt(x.getText(), defaultPos.getX());
        int yValue = MiscUtil.parseDefaultInt(y.getText(), defaultPos.getY());
        int zValue = MiscUtil.parseDefaultInt(z.getText(), defaultPos.getZ());
        posConsumer.accept(Optional.of(new BlockPos(xValue, yValue, zValue)));
    }

}
