package me.m0dii.nbteditor.util;

import com.google.gson.JsonParseException;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.serialization.Dynamic;
import me.m0dii.M0DevToolsClient;
import me.m0dii.nbteditor.multiversion.*;
import me.m0dii.nbteditor.multiversion.MVShaders.MVShaderAndLayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.zip.ZipException;

public class MiscUtil {

    public static final MinecraftClient client = MinecraftClient.getInstance();

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss", Locale.ROOT);

    /**
     * @param item
     * @param slot Format: container
     */
    public static void clickCreativeStack(ItemStack item, int slot) {
        if (M0DevToolsClient.SERVER_CONN.isEditingAllowed()) {
            MVMisc.sendC2SPacket(new CreativeInventoryActionC2SPacket(slot, item.copy()));
        }
    }

    public static void dropCreativeStack(ItemStack item) {
        if (M0DevToolsClient.SERVER_CONN.isEditingAllowed() && !item.isEmpty()) {
            MVMisc.sendC2SPacket(new CreativeInventoryActionC2SPacket(-1, item.copy()));
        }
    }

    public static void saveItem(Hand hand, ItemStack item) {
        client.player.setStackInHand(hand, item.copy());

        clickCreativeStack(item, hand == Hand.OFF_HAND ? SlotUtil.createOffHandInContainer() :
                SlotUtil.createHotbarInContainer(client.player.getInventory().selectedSlot));
    }

    public static void saveItem(EquipmentSlot equipment, ItemStack item) {
        switch (equipment) {
            case EquipmentSlot.MAINHAND -> {
                saveItem(Hand.MAIN_HAND, item);
            }
            case EquipmentSlot.OFFHAND -> {
                saveItem(Hand.OFF_HAND, item);
            }
            default -> {
                client.player.getInventory().armor.set(equipment.getEntitySlotId(), item.copy());
                clickCreativeStack(item, SlotUtil.createArmorInContainer(equipment));
            }
        }
    }

    /**
     * @param slot Format: inv
     * @param item
     */
    public static void saveItem(int slot, ItemStack item) {
        client.player.getInventory().setStack(slot, item.copy());
        clickCreativeStack(item, SlotUtil.invToContainer(slot));
    }

    public static void get(ItemStack item, boolean dropIfNoSpace) {
        PlayerInventory inv = client.player.getInventory();
        item = item.copy();

        int slot = inv.getOccupiedSlotWithRoomForStack(item);
        if (slot == -1) {
            slot = inv.getEmptySlot();
        }
        if (slot == -1) {
            if (dropIfNoSpace) {
                if (item.getCount() > item.getMaxCount()) {
                    item.setCount(item.getMaxCount());
                }
                dropCreativeStack(item);
            }
        } else {
            item.setCount(item.getCount() + inv.getStack(slot).getCount());
            int overflow = 0;
            if (item.getCount() > item.getMaxCount()) {
                overflow = item.getCount() - item.getMaxCount();
                item.setCount(item.getMaxCount());
            }
            saveItem(slot, item);
            if (overflow != 0) {
                item = item.copy();
                item.setCount(overflow);
                get(item, false);
            }
        }
    }

    public static void getWithMessage(ItemStack item) {
        get(item, true);
        client.player.sendMessage(TextInst.translatable("nbteditor.get.item").append(item.toHoverableText()), false);
    }

    public static void drawWrappingString(MatrixStack matrices,
                                          TextRenderer renderer,
                                          String text,
                                          int x,
                                          int y,
                                          int maxWidth,
                                          int color,
                                          boolean centerHorizontal,
                                          boolean centerVertical) {
        maxWidth = Math.max(maxWidth, renderer.getWidth("ww"));

        // Split into breaking spots
        List<String> parts = new ArrayList<>();
        List<Integer> spaces = new ArrayList<>();
        StringBuilder currentPart = new StringBuilder();
        boolean wasUpperCase = false;

        for (char c : text.toCharArray()) {
            if (c == ' ') {
                wasUpperCase = false;
                parts.add(currentPart.toString());
                currentPart.setLength(0);
                spaces.add(parts.size());
                continue;
            }

            boolean upperCase = Character.isUpperCase(c);
            if (upperCase != wasUpperCase && !currentPart.isEmpty()) { // Handle NBTEditor; output NBT, Editor; not N, B, T, Editor AND Handle MinionYT; output Minion YT
                if (wasUpperCase) {
                    parts.add(currentPart.substring(0, currentPart.length() - 1));
                    currentPart.delete(0, currentPart.length() - 1);
                } else {
                    parts.add(currentPart.toString());
                    currentPart.setLength(0);
                }
            }
            wasUpperCase = upperCase;
            currentPart.append(c);
        }

        if (!currentPart.isEmpty()) {
            parts.add(currentPart.toString());
        }

        // Generate lines, maximizing the number of parts per line
        List<String> lines = new ArrayList<>();
        String line = "";
        int i = 0;
        for (String part : parts) {
            String partAddition = (!line.isEmpty() && spaces.contains(i) ? " " : "") + part;
            if (renderer.getWidth(line + partAddition) > maxWidth) {
                if (!line.isEmpty()) {
                    lines.add(line);
                    line = "";
                }

                if (renderer.getWidth(part) <= maxWidth) {
                    line = part;
                } else {
                    while (true) {
                        int numChars = 1;
                        while (renderer.getWidth(part.substring(0, numChars)) < maxWidth)
                            numChars++;
                        numChars--;
                        lines.add(part.substring(0, numChars));
                        part = part.substring(numChars);
                        if (renderer.getWidth(part) < maxWidth) {
                            line = part;
                            break;
                        }
                    }
                }
            } else {
                line += partAddition;
            }
            i++;
        }

        if (!line.isEmpty()) {
            lines.add(line);
        }

        // Draw the lines
        for (i = 0; i < lines.size(); i++) {
            line = lines.get(i);
            int offsetY = i * renderer.fontHeight + (centerVertical ? -renderer.fontHeight * lines.size() / 2 : 0);

            if (centerHorizontal) {
                DrawableHelper.drawCenteredTextWithShadow(matrices, renderer, TextInst.of(line), x, y + offsetY, color);
            } else {
                DrawableHelper.drawTextWithShadow(matrices, renderer, TextInst.of(line), x, y + offsetY, color);
            }
        }
    }

    public static Text getBaseItemNameSafely(ItemStack item) {
        Text name = item.get(MVComponentType.ITEM_NAME);

        if (name != null) {
            return name;
        }

        return item.getName();
    }

    public static Text getNbtNameSafely(NbtCompound nbt, String key, Supplier<Text> defaultName) {
        if (nbt != null && nbt.contains(key, NbtElement.STRING_TYPE)) {
            try {
                Text text = TextInst.fromJson(nbt.getString(key));
                if (text != null) {
                    return text;
                }
            } catch (JsonParseException ignored) {
            }
        }
        return defaultName.get();
    }

    public static ItemStack copyAirable(ItemStack item) {
        ItemStack output = item.copyComponentsToNewStack(item.getItem(), item.getCount());
        output.setBobbingAnimationTime(item.getBobbingAnimationTime());
        return output;
    }

    public static ItemStack setType(Item type, ItemStack item, int count) {
        return item.copyComponentsToNewStack(type, count);
    }

    public static ItemStack setType(Item type, ItemStack item) {
        return setType(type, item, item.getCount());
    }

    public static NbtCompound readNBT(InputStream in) throws IOException {
        byte[] data = in.readAllBytes();
        try {
            return MVMisc.readCompressedNbt(new ByteArrayInputStream(data));
        } catch (ZipException e) {
            return MVMisc.readNbt(new ByteArrayInputStream(data));
        }
    }

    public static String getFormattedCurrentTime() {
        return DATE_TIME_FORMATTER.format(ZonedDateTime.now());
    }

    public static boolean equals(double a, double b, double epsilon) {
        return Math.abs(a - b) <= epsilon;
    }

    public static boolean equals(double a, double b) {
        return equals(a, b, 1E-5);
    }

    public static BufferedImage scaleImage(BufferedImage img, int width, int height) {
        Image temp = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = output.createGraphics();
        g.drawImage(temp, 0, 0, null);
        g.dispose();
        return output;
    }

    public static int[] getMousePos() {
        double scale = client.getWindow().getScaleFactor();
        int x = (int) (client.mouse.getX() / scale);
        int y = (int) (client.mouse.getY() / scale);
        return new int[]{x, y};
    }

    public static void mapMatrices(MatrixStack matrices,
                                   int fromX, int fromY, int fromWidth, int fromHeight,
                                   int toX, int toY, int toWidth, int toHeight) {
        matrices.translate(toX, toY, 0.0);
        matrices.scale((float) toWidth / fromWidth, (float) toHeight / fromHeight, 1);
        matrices.translate(-fromX, -fromY, 0.0);
    }


    public static Predicate<String> intPredicate(IntSupplier min, IntSupplier max, boolean allowEmpty) {
        return str -> {
            switch (str) {
                case "" -> {
                    return allowEmpty;
                }
                case "+" -> {
                    return allowEmpty && (max == null || max.getAsInt() >= 0);
                }
                case "-" -> {
                    return allowEmpty && (min == null || min.getAsInt() <= 0);
                }
            }
            try {
                int value = Integer.parseInt(str);
                return (min == null || min.getAsInt() <= value) && (max == null || value <= max.getAsInt());
            } catch (NumberFormatException e) {
                return false;
            }
        };
    }

    public static Predicate<String> intPredicate(Integer min, Integer max, boolean allowEmpty) {
        return intPredicate(() -> min, () -> max, allowEmpty);
    }

    public static Predicate<String> intPredicate() {
        return intPredicate((IntSupplier) null, null, true);
    }

    public static Integer parseOptionalInt(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static int parseDefaultInt(String str, int defaultValue) {
        Integer output = parseOptionalInt(str);
        if (output == null) {
            return defaultValue;
        }
        return output;
    }


    public static void fillShader(MatrixStack matrices, MVShaderAndLayer shader, Consumer<VertexConsumer> data, int x, int y, int width, int height) {
        int x1 = x;
        int y1 = y;
        int x2 = x + width;
        int y2 = y + height;

        MVMatrix4f matrix = MVMatrix4f.getPositionMatrix(matrices.peek());
        VertexConsumer vertexConsumer = MVMisc.beginDrawingShader(matrices, shader);

        matrix.applyToVertex(vertexConsumer, x1, y1, 0).texture(0, 0);
        data.accept(vertexConsumer);

        matrix.applyToVertex(vertexConsumer, x1, y2, 0).texture(0, 1);
        data.accept(vertexConsumer);

        matrix.applyToVertex(vertexConsumer, x2, y2, 0).texture(1, 1);
        data.accept(vertexConsumer);

        matrix.applyToVertex(vertexConsumer, x2, y1, 0).texture(1, 0);
        data.accept(vertexConsumer);

        RenderSystem.disableDepthTest();
        MVMisc.endDrawingShader(matrices);
        RenderSystem.enableDepthTest();
    }

    // Based on DataFixTypes
    @SuppressWarnings("unchecked")
    public static <T extends NbtElement> T update(TypeReference typeRef, T nbt, int oldVersion) {
        return (T) client.getDataFixer().update(typeRef, new Dynamic<>(NbtOps.INSTANCE, nbt), oldVersion, Version.getDataVersion()).getValue();
    }

    public static <T extends NbtElement> T updateDynamic(TypeReference typeRef, T nbt, NbtElement dataVersionTag, int defaultOldVersion) {
        int dataVersion = defaultOldVersion;
        if (dataVersionTag != null && dataVersionTag instanceof AbstractNbtNumber num) {
            dataVersion = num.intValue();
        } else if (dataVersion == -1) {
            return nbt;
        }
        return update(typeRef, nbt, dataVersion);
    }

    public static NbtCompound updateDynamic(TypeReference typeRef, NbtCompound nbt, int defaultOldVersion) {
        return updateDynamic(typeRef, nbt, nbt.get("DataVersion"), defaultOldVersion);
    }

    public static NbtCompound updateDynamic(TypeReference typeRef, NbtCompound nbt) {
        return updateDynamic(typeRef, nbt, -1);
    }

    public static NbtCompound fillId(NbtCompound nbt, String id) {
        if (!nbt.contains("id", NbtElement.STRING_TYPE)) {
            nbt.putString("id", id);
        }

        return nbt;
    }

    public static String addNamespace(String component) {
        if (component.contains(":")) {
            return component;
        }

        if (component.startsWith("!")) {
            return "!minecraft:" + component.substring(1);
        }

        return "minecraft:" + component;
    }

    public static <T> CompletableFuture<T> mergeFutures(List<CompletableFuture<T>> futures) {
        CompletableFuture<T> output = new CompletableFuture<>();
        output.thenAccept(value -> futures.forEach(future -> future.complete(value)));
        output.exceptionally(e -> {
            futures.forEach(future -> future.completeExceptionally(e));
            return null;
        });
        return output;
    }

    public static void setTextFieldValueSilently(TextFieldWidget widget, String text, boolean scrollToEnd) {
        widget.text = text;
        int cursor = (scrollToEnd ? text.length() : 0);
        widget.setSelectionStart(cursor);
        widget.setSelectionEnd(cursor);
    }

}

