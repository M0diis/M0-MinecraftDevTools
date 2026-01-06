package me.m0dii.modules.clickgui;

import me.m0dii.M0DevTools;
import me.m0dii.modules.chat.SecondaryChatModule;
import me.m0dii.modules.commandhistory.CommandHistoryModule;
import me.m0dii.modules.entityradar.EntityRadarModule;
import me.m0dii.modules.freecam.FreecamModule;
import me.m0dii.modules.fullbright.FullbrightModule;
import me.m0dii.modules.instabreak.InstaBreakModule;
import me.m0dii.modules.inventorymove.InventoryMoveModule;
import me.m0dii.modules.macros.gui.MacroKeybindOverlayModule;
import me.m0dii.modules.macros.gui.PendingMacrosOverlayModule;
import me.m0dii.modules.nbthud.NBTInfoHudOverlayModule;
import me.m0dii.modules.nbttooltip.NBTTooltipModule;
import me.m0dii.modules.overlays.*;
import me.m0dii.modules.uiutilities.UiUtilitiesModule;
import me.m0dii.modules.waypoints.WaypointModule;
import me.m0dii.modules.xray.XrayModule;
import me.m0dii.modules.zoom.ZoomModule;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Central registry for organizing modules into categories for the ClickGUI.
 */
public class ModuleRegistry {
    private static final List<ModuleCategory> categories = new ArrayList<>();

    private ModuleRegistry() {
        // Utility class, prevent instantiation
    }

    public static synchronized void initializeCategories() {
        if (!categories.isEmpty()) {
            return;
        }

        final BiConsumer<ModuleCategory, me.m0dii.modules.Module> safeAdd = (cat, mod) -> {
            try {
                if (mod != null) {
                    cat.addModule(mod);
                }
            } catch (Exception t) {
                M0DevTools.LOGGER.error("ModuleRegistry: failed to add module to category '{}': {}", cat.getName(), t);
            }
        };

        // Overlays Category
        ModuleCategory overlays = new ModuleCategory("Overlays");
        safeAdd.accept(overlays, RedstonePowerOverlayModule.INSTANCE);
        safeAdd.accept(overlays, SlimeChunkOverlayModule.INSTANCE);
        safeAdd.accept(overlays, LightLevelOverlayModule.INSTANCE);
        safeAdd.accept(overlays, ChunkBorderOverlayModule.INSTANCE);
        safeAdd.accept(overlays, StructureBoundingBoxOverlay.INSTANCE);
        categories.add(overlays);

        // HUD Category
        ModuleCategory hud = new ModuleCategory("HUD");
        safeAdd.accept(hud, MacroKeybindOverlayModule.INSTANCE);
        safeAdd.accept(hud, PendingMacrosOverlayModule.INSTANCE);
        safeAdd.accept(hud, NBTInfoHudOverlayModule.INSTANCE);
        safeAdd.accept(hud, EntityRadarModule.INSTANCE);
        safeAdd.accept(hud, ClickGuiModule.INSTANCE);
        categories.add(hud);

        // UI Category
        ModuleCategory ui = new ModuleCategory("UI");
        safeAdd.accept(ui, UiUtilitiesModule.INSTANCE);
        categories.add(ui);

        // Utilities Category
        ModuleCategory utilities = new ModuleCategory("Utilities");
        safeAdd.accept(utilities, XrayModule.INSTANCE);
        safeAdd.accept(utilities, CommandHistoryModule.INSTANCE);
        safeAdd.accept(utilities, InstaBreakModule.INSTANCE);
        safeAdd.accept(utilities, NBTTooltipModule.INSTANCE);
        safeAdd.accept(utilities, FullbrightModule.INSTANCE);
        categories.add(utilities);

        // Movement Category
        ModuleCategory movement = new ModuleCategory("Movement");
        safeAdd.accept(movement, InventoryMoveModule.INSTANCE);
        safeAdd.accept(movement, ZoomModule.INSTANCE);
        safeAdd.accept(movement, WaypointModule.INSTANCE);
        safeAdd.accept(movement, InventoryMoveModule.INSTANCE);
        safeAdd.accept(movement, FreecamModule.INSTANCE);
        categories.add(movement);

        // Chat Category
        ModuleCategory chat = new ModuleCategory("Chat");
        safeAdd.accept(chat, SecondaryChatModule.INSTANCE);
        categories.add(chat);
    }

    public static ModuleCategory[] getCategories() {
        if (categories.isEmpty()) {
            initializeCategories();
        }
        return categories.toArray(new ModuleCategory[0]);
    }

}
