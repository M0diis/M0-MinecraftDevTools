package me.m0dii.modules.waila;

import lombok.Getter;
import me.m0dii.modules.Module;
import me.m0dii.nbteditor.multiversion.networking.ClientNetworking;
import me.m0dii.nbteditor.multiversion.networking.ModPacket;
import me.m0dii.nbteditor.packets.*;
import me.m0dii.nbteditor.server.NBTEditorServer;
import me.m0dii.nbteditor.server.ServerMiscUtil;
import me.m0dii.nbteditor.util.MiscUtil;
import me.m0dii.utils.ModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.GameMode;
import net.minecraft.world.LightType;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class NBTInfoHudOverlayModule extends Module {

    public static final NBTInfoHudOverlayModule INSTANCE = new NBTInfoHudOverlayModule();

    private NBTInfoHudOverlayModule() {
        super("nbt_info_hud_overlay", "NBT Info HUD Overlay", false);
    }

    @Override
    public void register() {
        HudRenderCallback.EVENT.register(this::onHudRender);

        registerPressedKeybind("key.m0-dev-tools.toggle_block_inspector", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F10, client -> {
            toggleEnabled();
        });
    }

    private void onHudRender(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || !isEnabled()) {
            return;
        }

        List<String> lines = getInfoLines();
        if (lines.isEmpty()) {
            return;
        }

        TextRenderer tr = client.textRenderer;

        // Use inspector-specific configuration values with safe defaults and clamps
        double cfgScale = ModConfig.overlayInspectorTextScale;
        if (Double.isNaN(cfgScale) || cfgScale <= 0) {
            cfgScale = ModConfig.overlayInspectorTextScale = 1.0;
        }
        // clamp between 0.5 and 3.0 to avoid extreme sizes
        cfgScale = Math.max(0.5, Math.min(3.0, cfgScale));
        float scale = (float) cfgScale;

        int startX = Math.max(0, ModConfig.overlayInspectorMarginX);
        int startY = Math.max(0, ModConfig.overlayInspectorMarginY);
        int lineH = ModConfig.overlayInspectorLineHeight > 0 ? ModConfig.overlayInspectorLineHeight : 9; // logical line height at scale=1
        int padding = Math.max(0, ModConfig.overlayInspectorPadding);

        // Compute max width in unscaled text pixels, then scale for background
        int maxW = tr.getWidth("Inspector");
        for (String s : lines) maxW = Math.max(maxW, tr.getWidth(s));
        int maxWScaled = (int) Math.ceil(maxW * scale);

        // Compute total height in physical pixels
        int totalH = (int) Math.ceil((1 + lines.size()) * (lineH * scale) + (padding * 2));

        // Background panel (physical coords)
        ctx.fill(startX - padding, startY - padding, startX + maxWScaled + padding + 2, startY + totalH, 0x88000000);

        // Draw text scaled: scale the matrix, and pass coordinates divided by scale so text appears at physical coords
        int y = startY;
        ctx.getMatrices().push();
        ctx.getMatrices().scale(scale, scale, 1.0F);
        // draw title
        ctx.drawText(tr, "Inspector", (int) (startX / scale), (int) (y / scale), 0xFFFFFF, true);
        y += (int) Math.ceil(lineH * scale);

        for (String s : lines) {
            ctx.drawText(tr, s, (int) (startX / scale), (int) (y / scale), 0xC0C0C0, false);
            y += (int) Math.ceil(lineH * scale);
        }
        ctx.getMatrices().pop();
    }

    private static List<String> getInfoLines() {
        List<String> lines = new ArrayList<>();

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.world == null) {
            return List.of();
        }

        HitResult hit = client.crosshairTarget;
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            return List.of();
        }

        if (hit instanceof BlockHitResult blockHit) {
            var pos = blockHit.getBlockPos();
            BlockState state = client.world.getBlockState(pos);
            var block = state.getBlock();
            var biome = client.world.getBiome(pos).getIdAsString();
            int blockLight = client.world.getLightLevel(LightType.BLOCK, pos);
            int skyLight = client.world.getLightLevel(LightType.SKY, pos);
            int totalLight = client.world.getLightLevel(pos);
            int power = client.world.getReceivedRedstonePower(pos);

            lines.add("§f" + block.getName().getString());
            lines.add("§7Pos: §f" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
            lines.add("§7Light: §e" + totalLight + " §7(B:" + blockLight + " S:" + skyLight + ")");
            lines.add("§7Biome: §a" + biome);
            if (power > 0) {
                lines.add("§7Power: §c" + power);
            }

            // Show block states/properties
            var properties = state.getProperties();
            if (!properties.isEmpty()) {
                lines.add("§7States: §b" + properties.size());
                for (var property : properties) {
                    String propertyName = property.getName();
                    String propertyValue = getPropertyValueAsString(state, property);
                    lines.add(" §8- §7" + propertyName + ": §b" + propertyValue);
                }
            }

            // data for containers, crops, etc. can be added here
            if (state.hasBlockEntity()) {
                var blockEntity = client.world.getBlockEntity(pos);
                if (blockEntity != null) {
                    var nbt = blockEntity.createNbt(client.world.getRegistryManager());
                    if (!nbt.isEmpty()) {
                        lines.add("§7NBT: §e" + nbt.getSize() + " tags");
                        for (String key : nbt.getKeys()) {
                            var val = nbt.get(key);
                            lines.add(" §8- §7" + key + ": §f" + (val != null ? val.asString() : ""));
                        }
                    }
                }
            }
        }

        if (hit instanceof EntityHitResult entityHit) {
            var entity = entityHit.getEntity();
            String entityName = entity.getName().getString();
            String entityType = entity.getType().toString();
            if (entityType.contains(":")) {
                entityType = entityType.substring(entityType.lastIndexOf(":") + 1);
            }
            lines.add("§f" + entityName);
            lines.add("§7Type: §b" + entityType);
            lines.add("§7UUID: §8" + entity.getUuidAsString());
            if (entity instanceof net.minecraft.entity.LivingEntity living) {
                lines.add("§7Health: §c" + String.format("%.1f", living.getHealth()) + "/" + String.format("%.1f", living.getMaxHealth()));
            }

            var nbt = entity.writeNbt(new net.minecraft.nbt.NbtCompound());
            if (!nbt.isEmpty()) {
                lines.add("§7NBT: §e" + nbt.getSize() + " tags");
                for (String key : nbt.getKeys()) {
                    var val = nbt.get(key);
                    lines.add(" §8- §7" + key + ": §f" + (val != null ? val.asString() : ""));
                }
            }
        }

        return lines;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> String getPropertyValueAsString(BlockState state, net.minecraft.state.property.Property<?> property) {
        net.minecraft.state.property.Property<T> typedProperty = (net.minecraft.state.property.Property<T>) property;
        T value = state.get(typedProperty);
        return typedProperty.name(value);
    }

    public static class NBTEditorServerConn implements ClientNetworking.PlayNetworkStateEvents.Start, ClientNetworking.PlayNetworkStateEvents.Stop {

        public enum Status {
            DISCONNECTED,
            CLIENT_ONLY,
            INCOMPATIBLE,
            BOTH
        }

        @Getter
        private Status status;
        @Getter
        private boolean containerScreen;
        private int lastRequestId;
        private final Map<Integer, CompletableFuture<ModPacket>> requests;

        public NBTEditorServerConn() {
            status = Status.DISCONNECTED;
            containerScreen = false;
            lastRequestId = -1;
            requests = new HashMap<>();

            ClientNetworking.registerListener(ProtocolVersionS2CPacket.ID, this::onProtocolVersionPacket);
            ClientNetworking.registerListener(ContainerScreenS2CPacket.ID, this::onContainerScreenPacket);
            ClientNetworking.registerListener(ViewBlockS2CPacket.ID, this::receiveRequest);
            ClientNetworking.registerListener(ViewEntityS2CPacket.ID, this::receiveRequest);

            ClientNetworking.PlayNetworkStateEvents.Start.EVENT.register(this);
            ClientNetworking.PlayNetworkStateEvents.Stop.EVENT.register(this);
        }

        public boolean isEditingExpanded() {
            if (status != Status.BOTH) {
                return false;
            }

            GameMode gameMode = MiscUtil.client.interactionManager.getCurrentGameMode();
            return (gameMode.isCreative() || gameMode.isSurvivalLike()) && ServerMiscUtil.hasPermissionLevel(MiscUtil.client.player, 2);
        }

        public boolean isEditingAllowed() {
            return MiscUtil.client.interactionManager.getCurrentGameMode().isCreative() || isEditingExpanded();
        }

        public boolean isScreenEditable() {
            Screen screen = MiscUtil.client.currentScreen;
            return screen instanceof CreativeInventoryScreen ||
                    isEditingExpanded() && (screen instanceof InventoryScreen || containerScreen);
        }

        public void closeContainerScreen() {
            containerScreen = false;
        }

        public <T extends ModPacket> CompletableFuture<Optional<T>> sendRequest(Function<Integer, ModPacket> packet, Class<T> responseType) {
            if (!isEditingExpanded()) {
                return CompletableFuture.completedFuture(Optional.empty());
            }

            CompletableFuture<ModPacket> future = new CompletableFuture<>();
            int requestId = ++lastRequestId;
            requests.put(requestId, future);
            ClientNetworking.send(packet.apply(requestId));

            return future.thenApply(response -> {
                if (responseType.isInstance(response)) {
                    return Optional.of(responseType.cast(response));
                }

                return Optional.<T>empty();
            }).completeOnTimeout(Optional.empty(), 1000, TimeUnit.MILLISECONDS).thenApply(output -> {
                requests.remove(requestId);
                return output;
            });
        }

        private void receiveRequest(ResponsePacket packet) {
            CompletableFuture<ModPacket> receiver = requests.remove(packet.requestId());
            if (receiver != null) {
                receiver.complete(packet);
            }
        }

        @Override
        public void onPlayStart(ClientPlayNetworkHandler networkHandler) {
            status = Status.CLIENT_ONLY;
        }

        @Override
        public void onPlayStop() {
            status = Status.DISCONNECTED;
        }

        private void onProtocolVersionPacket(ProtocolVersionS2CPacket packet) {
            if (packet.version() == NBTEditorServer.PROTOCOL_VERSION) {
                status = Status.BOTH;
            } else {
                status = Status.INCOMPATIBLE;
            }
        }

        private void onContainerScreenPacket(ContainerScreenS2CPacket packet) {
            containerScreen = true;
        }

    }
}
