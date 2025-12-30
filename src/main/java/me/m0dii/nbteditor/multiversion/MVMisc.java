package me.m0dii.nbteditor.multiversion;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.serialization.JsonOps;
import me.m0dii.nbteditor.multiversion.MVShaders.MVShaderAndLayer;
import me.m0dii.nbteditor.multiversion.commands.ClientCommandRegistrationCallback;
import me.m0dii.nbteditor.multiversion.commands.FabricClientCommandSource;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockStateArgumentType;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.command.argument.TextArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.network.packet.Packet;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.joml.Vector2ic;

import java.awt.*;
import java.io.*;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MVMisc {

    public static Object registryAccess;

    private MVMisc() {
    }

    public static Optional<InputStream> getResource(Identifier id) throws IOException {
        try {
            return MiscUtil.client.getResourceManager().getResource(id).map(resource -> {
                try {
                    return resource.getInputStream();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            if (e.getMessage() != null) {
                IOException checkedE = new IOException(e.getMessage(), e.getCause());
                checkedE.setStackTrace(e.getStackTrace());
                throw checkedE;
            }
            throw e.getCause();
        }
    }

    public static ItemStackArgumentType getItemStackArg() {
        return ItemStackArgumentType.itemStack((CommandRegistryAccess) registryAccess);
    }

    public static BlockStateArgumentType getBlockStateArg() {
        return BlockStateArgumentType.blockState((CommandRegistryAccess) registryAccess);
    }

    public static TextArgumentType getTextArg() {
        return TextArgumentType.text((CommandRegistryAccess) registryAccess);
    }

    public static void registerCommands(Consumer<CommandDispatcher<FabricClientCommandSource>> callback) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> {
            registryAccess = access;
            callback.accept(dispatcher);
        });
    }

    public static ButtonWidget newButton(int x, int y, int width, int height, Text message, ButtonWidget.PressAction onPress, MVTooltip tooltip) {
        Tooltip newTooltip = (tooltip == null ? null : tooltip.toNewTooltip());
        return ButtonWidget.builder(message, onPress).dimensions(x, y, width, height).tooltip(newTooltip).build();
    }

    public static ButtonWidget newButton(int x, int y, int width, int height, Text message, ButtonWidget.PressAction onPress) {
        return newButton(x, y, width, height, message, onPress, null);
    }

    public static ButtonWidget newTexturedButton(int x, int y, int width, int height, int hoveredVOffset, Identifier img, ButtonWidget.PressAction onPress, MVTooltip tooltip) {
        ButtonWidget output = new MVTexturedButtonWidget(
                x,
                y,
                width,
                height,
                0,
                0,
                hoveredVOffset,
                img,
                width,
                height + hoveredVOffset,
                onPress);
        if (tooltip != null) {
            output.setTooltip(tooltip.toNewTooltip());
        }
        return output;
    }

    public static boolean isCreativeInventoryTabSelected() {
        return MiscUtil.client.currentScreen instanceof CreativeInventoryScreen screen && screen.isInventoryTabSelected();
    }

    public static boolean isValidChar(char c) {
        return c != '§' && c >= ' ' && c != 127;
    }

    public static String stripInvalidChars(String str, boolean allowLinebreaks) {
        StringBuilder output = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (isValidChar(c)) {
                output.append(c);
            } else if (allowLinebreaks && c == '\n') {
                output.append(c);
            }
        }
        return output.toString();
    }

    public static String getContent(Text text) {
        StringBuilder output = new StringBuilder();
        text.getContent().visit(str -> {
            output.append(str);
            return Optional.empty();
        });
        return output.toString();

    }

    public static Vector2ic getPosition(Object positioner, Screen screen, int x, int y, int width, int height) {
        return ((TooltipPositioner) positioner).getPosition(
                MiscUtil.client.getWindow().getScaledWidth(),
                MiscUtil.client.getWindow().getScaledHeight(), x, y, width, height
        );
    }

    public static void sendC2SPacket(Packet<?> packet) {
        MiscUtil.client.getNetworkHandler().sendPacket(packet);
    }

    public static NbtCompound nbtInternal(Supplier<NbtCompound> newWrite) throws IOException {
        try {
            return newWrite.get();
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    public static void nbtInternal(Runnable newWrite) throws IOException {
        nbtInternal(() -> {
            newWrite.run();
            return null;
        });
    }

    public static NbtCompound readNbt(InputStream stream) throws IOException {
        return nbtInternal(() -> {
            try {
                return NbtIo.readCompound(new DataInputStream(stream), NbtSizeTracker.ofUnlimitedBytes());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public static NbtCompound readCompressedNbt(InputStream stream) throws IOException {
        return nbtInternal(() -> {
            try {
                return NbtIo.readCompressed(stream, NbtSizeTracker.ofUnlimitedBytes());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public static void writeNbt(NbtCompound nbt, OutputStream stream) throws IOException {
        nbtInternal(() -> {
            try {
                NbtIo.write(nbt, new DataOutputStream(stream));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public static void writeCompressedNbt(NbtCompound nbt, OutputStream stream) throws IOException {
        nbtInternal(() -> {
            try {
                NbtIo.writeCompressed(nbt, stream);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public static void writeCompressedNbt(NbtCompound nbt, File file) throws IOException {
        try (FileOutputStream stream = new FileOutputStream(file)) {
            writeCompressedNbt(nbt, stream);
        }
    }

    public static String getClickEventActionName(ClickEvent.Action action) {
        // Should be #getName() until 1.20.2 and #asString() at and after 1.20.3
        // But this seems to be equivalent (at least currently)
        return action.name().toLowerCase();
    }

    public static String getHoverEventActionName(HoverEvent.Action<?> action) {
        // Should be #getName() until 1.20.2 and #asString() at and after 1.20.3
        // But this seems to be equivalent (at least currently)
        String str = action.toString();
        return str.substring("<action ".length(), str.length() - ">".length());
    }

    public static ClickEvent.Action getClickEventAction(String name) {
        // Should be .byName() until 1.20.2 and doesn't have a clear replacement at and after 1.20.3
        // But this seems to be equivalent (at least currently)
        return ClickEvent.Action.valueOf(name.toUpperCase());
    }

    public static JsonElement getHoverEventContentsJson(HoverEvent event) {
        return HoverEvent.CODEC.encodeStart(JsonOps.INSTANCE, event).result().orElseThrow().getAsJsonObject().get("contents");
    }

    public static HoverEvent getHoverEvent(JsonObject json) {
        return HoverEvent.CODEC.parse(JsonOps.INSTANCE, json).result().orElseThrow();
    }

    public static VertexConsumer beginDrawingShader(MatrixStack matrices, MVShaderAndLayer shader) {
        return DrawableHelper.getDrawContext(matrices).vertexConsumers.getBuffer(shader.layer());
    }

    public static void endDrawingShader(MatrixStack matrices) {
        DrawableHelper.getDrawContext(matrices).vertexConsumers.draw();
    }

    public static void setCursor(TextFieldWidget textField, int cursor) {
        textField.setCursor(cursor, false);
    }

    public static void renderBlock(BlockRenderManager renderer, BlockState state, BlockPos pos, BlockRenderView world, MatrixStack matrices, VertexConsumer vertexConsumer, boolean cull) {
        renderer.renderBlock(state, pos, world, matrices, vertexConsumer, cull, Random.create());
    }

    public static EntityType<?> getEntityType(ItemStack item) {
        SpawnEggItem spawnEggItem = (SpawnEggItem) item.getItem();
        return spawnEggItem.getEntityType(DynamicRegistryManagerHolder.get(), item);
    }

    public static StatusEffectInstance newStatusEffectInstance(StatusEffect effect, int duration) {
        return new StatusEffectInstance(Registries.STATUS_EFFECT.getEntry(effect), duration);
    }

    public static StatusEffectInstance newStatusEffectInstance(StatusEffect effect, int duration, int amplifier, boolean ambient, boolean showParticles, boolean showIcon) {
        return new StatusEffectInstance(Registries.STATUS_EFFECT.getEntry(effect), duration, amplifier, ambient, showParticles, showIcon);
    }

    public static StatusEffect getEffectType(StatusEffectInstance effect) {
        return effect.getEffectType().value();
    }

    public static void showToast(Text title, Text description) {
        MiscUtil.client.getToastManager().add(new SystemToast(SystemToast.Type.PACK_LOAD_FAILURE, title, description));
    }

    public static void setInitialFocus(Screen screen, Element element, Consumer<Element> superCall) {
        superCall.accept(element);
        screen.setFocused(element);
    }

    public static float getTickDelta() {
        return MiscUtil.client.getRenderTickCounter().getTickDelta(true);
    }

    public static ServerCommandSource getCommandSource(Entity entity) {
        return new ServerCommandSource(
                CommandOutput.DUMMY,
                entity.getPos(),
                entity.getRotationClient(),
                null,
                0,
                entity.getName().getString(),
                entity.getDisplayName(),
                null,
                entity
        );
    }

    @SuppressWarnings("deprecation")
    public static void addBlockEntityNbtWithoutXYZ(ItemStack item, BlockEntity entity) {
        NbtCompound blockEntityTag = entity.createComponentlessNbtWithIdentifyingData(DynamicRegistryManagerHolder.get());
        blockEntityTag.remove("x");
        blockEntityTag.remove("y");
        blockEntityTag.remove("z");
        entity.removeFromCopiedStackNbt(blockEntityTag);
        BlockItem.setBlockEntityData(item, entity.getType(), blockEntityTag);
        item.applyComponentsFrom(entity.createComponentMap());
    }

    public static <T extends BlockEntity> boolean renderBlockEntity(BlockEntityRenderDispatcher dispatcher,
                                                                    T entity,
                                                                    float tickDelta,
                                                                    MatrixStack matrices,
                                                                    VertexConsumerProvider provider) {
        BlockEntityRenderer<T> renderer = dispatcher.get(entity);
        if (renderer == null) {
            return true;
        }

        try {
            renderer.render(entity, tickDelta, matrices, provider, 0xF000F0, OverlayTexture.DEFAULT_UV);
        } catch (Exception e) {
            CrashReport report = CrashReport.create(e, "Rendering Block Entity");
            CrashReportSection entitySection = report.addElement("Block Entity Details");
            entity.populateCrashReport(entitySection);
            throw new CrashException(report);
        }
        return false;
    }

    public static int scaleRgb(int argb, double scale) {
        Color color = new Color(argb, true);

        int r = (int) (color.getRed() * scale);
        int g = (int) (color.getGreen() * scale);
        int b = (int) (color.getBlue() * scale);

        return new Color(r, g, b, color.getAlpha()).getRGB();
    }
}
