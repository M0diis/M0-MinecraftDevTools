package me.m0dii.nbteditor.screens.widgets;

import me.m0dii.nbteditor.multiversion.*;
import me.m0dii.nbteditor.screens.OverlaySupportingScreen;
import me.m0dii.nbteditor.screens.WidgetScreen;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ImageToLoreWidget extends GroupWidget implements InitializableOverlay<Screen> {

    private final Consumer<Optional<ImageToLoreOptions>> optionsConsumer;
    private final TextRenderer textRenderer;
    private int width;
    private int height;
    private NamedTextFieldWidget imgWidth;
    private NamedTextFieldWidget imgHeight;

    public ImageToLoreWidget(Consumer<Optional<ImageToLoreOptions>> optionsConsumer) {
        this.optionsConsumer = optionsConsumer;
        this.textRenderer = MiscUtil.client.textRenderer;
    }

    public static List<Text> imageToLore(BufferedImage img, int width, int height) {
        img = MiscUtil.scaleImage(img, width, height);
        List<Text> output = new ArrayList<>();
        for (int line = 0; line < height; line++) {
            EditableText lineText = TextInst.literal("").styled(style -> style.withItalic(false));
            for (int i = 0; i < width; i++) {
                final int color = img.getRGB(i, line) & 0xFFFFFF;
                lineText.append(TextInst.literal("█").styled(style -> style.withColor(color)));
            }
            output.add(lineText);
        }
        return output;
    }

    public static boolean openImportFiles(List<Path> paths, BiConsumer<File, List<Text>> loreConsumers, Runnable onDone) {
        Map<File, BufferedImage> imgs = new LinkedHashMap<>();
        for (Path path : paths) {
            File file = path.toFile();
            try {
                BufferedImage img = ImageIO.read(file);
                if (img != null) {
                    imgs.put(file, img);
                }
            } catch (IOException ignored) {
            }
        }

        if (imgs.isEmpty()) {
            return false;
        }

        WidgetScreen.setOverlayOrScreen(new ImageToLoreWidget(optional -> {
            OverlaySupportingScreen.setOverlayStatic(null);

            if (optional.isEmpty()) {
                return;
            }

            ImageToLoreOptions options = optional.get();

            imgs.forEach((file, img) -> {
                int width = img.getWidth();
                int height = img.getHeight();
                if (options.width() != null && options.height() != null) {
                    width = options.width();
                    height = options.height();
                } else if (options.width() != null) {
                    height = (int) ((double) options.width() / width * height);
                    width = options.width();
                } else if (options.height() != null) {
                    width = (int) ((double) options.height() / height * width);
                    height = options.height();
                }

                loreConsumers.accept(file, imageToLore(img, width, height));
            });

            onDone.run();
        }), 200, true);

        return true;
    }

    @Override
    public void init(Screen parent, int width, int height) {
        clearWidgets();

        this.width = width;
        this.height = height;

        String prevImgWidth = (imgWidth == null ? null : imgWidth.getText());
        String prevImgHeight = (imgHeight == null ? null : imgHeight.getText());

        imgWidth = addWidget(new NamedTextFieldWidget(width / 2 - 102, height / 2 - 18, 100, 16)
                .name(TextInst.translatable("nbteditor.img_to_lore.width")));
        imgHeight = addWidget(new NamedTextFieldWidget(width / 2 + 2, height / 2 - 18, 100, 16)
                .name(TextInst.translatable("nbteditor.img_to_lore.height")));

        imgWidth.setTextPredicate(MiscUtil.intPredicate(1, Integer.MAX_VALUE, true));
        imgHeight.setTextPredicate(MiscUtil.intPredicate(1, Integer.MAX_VALUE, true));

        if (prevImgWidth != null) {
            imgWidth.setText(prevImgWidth);
        }
        if (prevImgHeight != null) {
            imgHeight.setText(prevImgHeight);
        }

        addWidget(MVMisc.newButton(width / 2 - 102, height / 2 + 2, 100, 20, ScreenTexts.DONE, btn -> {
            optionsConsumer.accept(Optional.of(new ImageToLoreOptions(
                    MiscUtil.parseOptionalInt(imgWidth.getText()), MiscUtil.parseOptionalInt(imgHeight.getText()))));
        }));
        addWidget(MVMisc.newButton(width / 2 + 2, height / 2 + 2, 100, 20, ScreenTexts.CANCEL, btn -> {
            optionsConsumer.accept(Optional.empty());
        }));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (!(MiscUtil.client.currentScreen instanceof WidgetScreen)) {
            DrawableHelper.fill(matrices, width / 2 - 102 - 16, height / 2 - 18 - 16, width / 2 + 102 + 16, height / 2 + 22 + 16, 0xC8101010);
        }
        super.render(matrices, mouseX, mouseY, delta);
        DrawableHelper.drawCenteredTextWithShadow(matrices, textRenderer, TextInst.translatable("nbteditor.img_to_lore"),
                width / 2, height / 2 - textRenderer.fontHeight - 22, -1);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            OverlaySupportingScreen.setOverlayStatic(null);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            optionsConsumer.accept(Optional.of(new ImageToLoreOptions(
                    MiscUtil.parseOptionalInt(imgWidth.getText()), MiscUtil.parseOptionalInt(imgHeight.getText()))));
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public record ImageToLoreOptions(Integer width, Integer height) {
    }

}
