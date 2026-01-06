package me.m0dii.modules.clickgui;

import me.m0dii.M0DevTools;
import me.m0dii.modules.Module;
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

/**
 * Central registry for organizing modules into categories for the ClickGUI.
 */
public class ModuleRegistry {
    private static final List<ModuleCategory> categories = new ArrayList<>();

    private ModuleRegistry() {
        // Utility class
    }

    private static void safeAdd(ModuleCategory cat, Module mod) {
        try {
            if (mod != null) {
                cat.addModule(mod);
            }
        } catch (Exception t) {
            M0DevTools.LOGGER.error("ModuleRegistry: failed to add module to category '{}': {}", cat.getName(), t);
        }
    }

    public static synchronized void initializeCategories() {
        if (!categories.isEmpty()) {
            return;
        }

        ModuleCategory overlays = new ModuleCategory("Overlays");
        safeAdd(overlays, RedstonePowerOverlayModule.INSTANCE);
        safeAdd(overlays, SlimeChunkOverlayModule.INSTANCE);
        safeAdd(overlays, LightLevelOverlayModule.INSTANCE);
        safeAdd(overlays, ChunkBorderOverlayModule.INSTANCE);
        safeAdd(overlays, StructureBoundingBoxOverlay.INSTANCE);
        categories.add(overlays);

        ModuleCategory hud = new ModuleCategory("HUD");
        safeAdd(hud, MacroKeybindOverlayModule.INSTANCE);
        safeAdd(hud, PendingMacrosOverlayModule.INSTANCE);
        safeAdd(hud, NBTInfoHudOverlayModule.INSTANCE);
        safeAdd(hud, EntityRadarModule.INSTANCE);
        safeAdd(hud, ClickGuiModule.INSTANCE);
        categories.add(hud);

        ModuleCategory ui = new ModuleCategory("UI");
        safeAdd(ui, UiUtilitiesModule.INSTANCE);
        categories.add(ui);

        ModuleCategory utilities = new ModuleCategory("Utilities");
        safeAdd(utilities, XrayModule.INSTANCE);
        safeAdd(utilities, CommandHistoryModule.INSTANCE);
        safeAdd(utilities, InstaBreakModule.INSTANCE);
        safeAdd(utilities, NBTTooltipModule.INSTANCE);
        safeAdd(utilities, FullbrightModule.INSTANCE);
        categories.add(utilities);

        ModuleCategory movement = new ModuleCategory("Movement");
        safeAdd(movement, InventoryMoveModule.INSTANCE);
        safeAdd(movement, ZoomModule.INSTANCE);
        safeAdd(movement, WaypointModule.INSTANCE);
        safeAdd(movement, InventoryMoveModule.INSTANCE);
        safeAdd(movement, FreecamModule.INSTANCE);
        categories.add(movement);

        ModuleCategory chat = new ModuleCategory("Chat");
        safeAdd(chat, SecondaryChatModule.INSTANCE);
        categories.add(chat);
    }

    public static ModuleCategory[] getCategories() {
        if (categories.isEmpty()) {
            initializeCategories();
        }
        return categories.toArray(new ModuleCategory[0]);
    }

}
