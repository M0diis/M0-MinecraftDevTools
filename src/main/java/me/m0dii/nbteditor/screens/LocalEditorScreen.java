package me.m0dii.nbteditor.screens;

import lombok.Getter;
import me.m0dii.nbteditor.localnbt.LocalNBT;
import me.m0dii.nbteditor.multiversion.MVMisc;
import me.m0dii.nbteditor.multiversion.MVTooltip;
import me.m0dii.nbteditor.multiversion.TextInst;
import me.m0dii.nbteditor.nbtreferences.NBTReference;
import me.m0dii.nbteditor.nbtreferences.itemreferences.ItemReference;
import me.m0dii.nbteditor.screens.factories.LocalFactoryScreen;
import me.m0dii.nbteditor.screens.util.FancyConfirmScreen;
import me.m0dii.nbteditor.screens.widgets.AlertWidget;
import me.m0dii.nbteditor.screens.widgets.NamedTextFieldWidget;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.function.Function;

public abstract class LocalEditorScreen<L extends LocalNBT> extends OverlaySupportingScreen {

    protected final NBTReference<L> ref;
    protected L localNBT;
    protected L savedLocalNBT;
    protected NamedTextFieldWidget name;

    @Getter
    private boolean saved;
    private ButtonWidget saveBtn;

    protected LocalEditorScreen(Text title, NBTReference<L> ref) {
        super(title);
        this.ref = ref;
        this.savedLocalNBT = LocalNBT.copy(ref.getLocalNBT());
        this.localNBT = LocalNBT.copy(savedLocalNBT);
        this.saved = true;
    }

    protected boolean isNameEditable() {
        return false;
    }

    protected boolean isSaveRequired() {
        return true;
    }

    protected FactoryLink<L> getFactoryLink() {
        return new FactoryLink<>("nbteditor.factory", LocalFactoryScreen::new);
    }

    @Override
    protected final void init() {
        super.init();

        name = new NamedTextFieldWidget(16 + (32 + 8) * 2, 16 + 8, 208, 16) {
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (isNameEditable()) {
                    return super.mouseClicked(mouseX, mouseY, button);
                } else {
                    return false;
                }
            }
        }.name(TextInst.translatable("nbteditor.editor.name"));

        name.setMaxLength(Integer.MAX_VALUE);
        name.setText(localNBT.getName().getString());
        name.setEditable(isNameEditable());

        addDrawableChild(name);

        if (isSaveRequired()) {
            saveBtn = addDrawableChild(MVMisc.newButton(16 + (32 + 8) * 2 + 208 + 8, 16 + 6, 75, 20, TextInst.translatable("nbteditor.editor.save"), btn -> {
                save();
            }));
            saveBtn.active = !saved;
        }

        FactoryLink<L> link = getFactoryLink();
        if (link != null) {
            addDrawableChild(MVMisc.newTexturedButton(width - 36, 22, 20, 20, 20,
                    LocalFactoryScreen.FACTORY_ICON,
                    btn -> closeSafely(() -> client.setScreen(link.factory().apply(ItemReference.toItemStackRef(ref)))),
                    new MVTooltip(link.langName())));
        }

        initEditor();
    }

    protected void initEditor() {
    }

    @Override
    public final void renderMain(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.renderBackground(matrices);
        preRenderEditor(matrices, mouseX, mouseY, delta);
        super.renderMain(matrices, mouseX, mouseY, delta);
        renderEditor(matrices, mouseX, mouseY, delta);
        renderPreview(matrices, delta);
    }

    private void renderPreview(MatrixStack matrices, float tickDelta) {
        int x = 16 + 32 + 8;
        int y = 16;
        int scaleX = 2;
        int scaleY = 2;

        x /= scaleX;
        y /= scaleY;

        matrices.push();
        matrices.translate(0.0D, 0.0D, 32.0D);
        matrices.scale(scaleX, scaleY, 1);

        localNBT.renderIcon(matrices, x, y, tickDelta);

        matrices.pop();
    }

    protected void preRenderEditor(MatrixStack matrices, int mouseX, int mouseY, float delta) {
    }

    protected void renderEditor(MatrixStack matrices, int mouseX, int mouseY, float delta) {
    }

    protected void renderTip(MatrixStack matrices, String langHint) {
        if (!ConfigScreen.isKeybindsHidden()) {
            int x = 16 + (32 + 8) * 2 + (100 + 8) * 2;
            MiscUtil.drawWrappingString(matrices, textRenderer, TextInst.translatable(langHint).getString(),
                    16 + (32 + 8) * 2 + (100 + 8) * 2, 16 + 6 + 10, width - x - 8 - 20 - 8, -1, false, true);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (getOverlay() != null) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        if (hasControlDown() && !hasShiftDown() && !hasAltDown() && keyCode == GLFW.GLFW_KEY_S) {
            save();
            return true;
        }

        return name.keyPressed(keyCode, scanCode, modifiers);
    }

    protected void setSaved(boolean saved) {
        this.saved = saved;
        if (saveBtn != null) {
            saveBtn.active = !saved;
        }
    }

    protected boolean save() {
        if (ref.exists()) {
            savedLocalNBT = LocalNBT.copy(localNBT);
            saveBtn.setMessage(TextInst.translatable("nbteditor.editor.saving"));
            setSaved(true);
            ref.saveLocalNBT(savedLocalNBT, () -> saveBtn.setMessage(TextInst.translatable("nbteditor.editor.save")));
        } else {
            localNBT.toItem().ifPresentOrElse(item -> {
                savedLocalNBT = LocalNBT.copy(localNBT);
                setSaved(true);
                saveBtn.setMessage(TextInst.translatable("nbteditor.editor.save"));
            }, () -> setOverlay(new AlertWidget(() -> setOverlay(null), TextInst.translatable("nbteditor.editor.ref_broken")), 500));
        }
        return true;
    }

    protected void checkSave() {
        localNBT.getOrCreateNBT(); // Make sure both items have NBT defined, so no NBT and empty NBT comes out equal
        savedLocalNBT.getOrCreateNBT();
        setSaved(localNBT.equals(savedLocalNBT));
    }

    @Override
    public void close() {
        closeSafely(ref::showParent);
    }

    protected void closeSafely(Runnable onClose) {
        if (saved) {
            onClose.run();
        } else {
            client.setScreen(new FancyConfirmScreen(value -> {
                if (!value || save()) {
                    onClose.run();
                }
            }, TextInst.translatable("nbteditor.editor.unsaved.title"), TextInst.translatable("nbteditor.editor.unsaved.desc"),
                    TextInst.translatable("nbteditor.editor.unsaved.yes"), TextInst.translatable("nbteditor.editor.unsaved.no")));
        }
    }

    public record FactoryLink<L extends LocalNBT>(String langName, Function<NBTReference<L>, Screen> factory) {

    }

}
