package me.m0dii.modules.placement;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import me.m0dii.M0DevToolsClient;
import me.m0dii.modules.Module;
import me.m0dii.modules.PersistentSettings;
import me.m0dii.modules.state.ModuleStateStore;
import me.m0dii.utils.KeybindCatalog;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Placement Restriction: while you hold the use key, blocks only go on the plane, face, column or line set by
 * the first block. Client-side only; it just skips placements outside the shape.
 */
public class PlacementRestrictionModule extends Module implements PersistentSettings {
    public static final PlacementRestrictionModule INSTANCE = new PlacementRestrictionModule();

    private final PlacementRestriction restriction = new PlacementRestriction();
    private PlacementRestriction.Mode mode = PlacementRestriction.Mode.PLANE;

    private PlacementRestrictionModule() {
        super("placement_restriction", "Placement Restriction", false);
    }

    @Override
    public void register() {
        registerPressedKeybind(KeybindCatalog.PLACEMENT_RESTRICTION_CYCLE.translationKey(),
                InputConstants.Type.KEYBOARD,
                KeybindCatalog.PLACEMENT_RESTRICTION_CYCLE.defaultKey(),
                client -> {
                    String text = cycleMode();
                    if (client.player != null) {
                        client.player.sendOverlayMessage(Component.literal(text));
                    }
                });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!client.options.keyUse.isDown()) {
                restriction.reset();
            }
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            var root = ClientCommands.literal("placement").executes(ctx -> {
                ctx.getSource().sendFeedback(Component.literal("[Placement] " + status()));
                return 1;
            });
            for (PlacementRestriction.Mode option : PlacementRestriction.Mode.values()) {
                root.then(ClientCommands.literal(option.label()).executes(ctx -> {
                    setMode(option);
                    ctx.getSource().sendFeedback(Component.literal("[Placement] " + status()));
                    return 1;
                }));
            }
            dispatcher.register(root);
        });
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(M0DevToolsClient.MOD_ID, "placement_restriction"),
                this::renderHud);
    }

    private String status() {
        return isEnabled() ? "Restricting placement: " + mode.label() + "." : "Off (last mode: " + mode.label() + ").";
    }

    private void setMode(PlacementRestriction.Mode newMode) {
        if (newMode == PlacementRestriction.Mode.OFF) {
            setEnabled(false);
        } else {
            mode = newMode;
            if (!isEnabled()) {
                setEnabled(true);
            }
        }
        restriction.reset();
        ModuleStateStore.onSettingsChanged(this);
    }

    private String cycleMode() {
        PlacementRestriction.Mode next = isEnabled() ? mode.next() : PlacementRestriction.Mode.PLANE;
        setMode(next);
        return "Placement restriction: " + (isEnabled() ? mode.label() : "off");
    }

    /** Called from the interaction mixin before a block is used/placed. False skips the placement. */
    public boolean allowPlacement(LocalPlayer player, InteractionHand hand, BlockHitResult hit) {
        if (!isEnabled() || player == null || hit == null) {
            return true;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof BlockItem)) {
            return true;
        }
        BlockPos place = new BlockPlaceContext(player, hand, stack, hit).getClickedPos();
        return restriction.allow(mode,
                new PlacementRestriction.Pos(place.getX(), place.getY(), place.getZ()),
                hit.getDirection().getName(),
                hit.getDirection().getAxis().getName().charAt(0));
    }

    private void renderHud(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        if (!isEnabled() || client.player == null || client.gui.screen() != null) {
            return;
        }
        boolean holdingBlock = client.player.getMainHandItem().getItem() instanceof BlockItem
                || client.player.getOffhandItem().getItem() instanceof BlockItem;
        if (!holdingBlock) {
            return;
        }
        String text = "⊞ " + restriction.describe(mode);
        int x = client.getWindow().getGuiScaledWidth() / 2 + 8;
        int y = client.getWindow().getGuiScaledHeight() / 2 + 8;
        context.text(client.font, text, x, y, restriction.hasAnchor() ? 0xFFFFD37A : 0xFFAAAAAA, true);
    }

    @Override
    public List<String> getSettingsDisplay() {
        List<String> settings = new ArrayList<>();
        settings.add("Toggle: " + (isEnabled() ? "ON" : "OFF"));
        settings.add("Mode: " + mode.label());
        return settings;
    }

    @Override
    public void onSettingSelected(int settingIndex) {
        if (settingIndex == 0) {
            toggleEnabled();
        } else if (settingIndex == 1) {
            PlacementRestriction.Mode next = mode.next();
            mode = next == PlacementRestriction.Mode.OFF ? next.next() : next;
            restriction.reset();
        }
    }

    @Override
    public JsonObject saveSettings() {
        JsonObject json = new JsonObject();
        json.addProperty("mode", mode.name());
        return json;
    }

    @Override
    public void loadSettings(JsonObject settings) {
        PlacementRestriction.Mode loaded = PlacementRestriction.Mode.parse(PersistentSettings.readString(settings, "mode", mode.name()));
        if (loaded != null && loaded != PlacementRestriction.Mode.OFF) {
            mode = loaded;
        }
    }
}
