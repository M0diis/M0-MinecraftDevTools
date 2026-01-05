package me.m0dii.modules.clickgui;

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
        // Utility class, prevent instantiation
    }

    static {
        initializeCategories();
    }

    private static void initializeCategories() {
        // Overlays Category
        ModuleCategory overlays = new ModuleCategory("Overlays");
        overlays.addModule(RedstonePowerOverlayModule.INSTANCE);
        overlays.addModule(SlimeChunkOverlayModule.INSTANCE);
        overlays.addModule(LightLevelOverlayModule.INSTANCE);
        overlays.addModule(ChunkBorderOverlayModule.INSTANCE);
        overlays.addModule(StructureBoundingBoxOverlay.INSTANCE);
        categories.add(overlays);

        // HUD Category
        ModuleCategory hud = new ModuleCategory("HUD");
        hud.addModule(MacroKeybindOverlayModule.INSTANCE);
        hud.addModule(PendingMacrosOverlayModule.INSTANCE);
        hud.addModule(NBTInfoHudOverlayModule.INSTANCE);
        hud.addModule(EntityRadarModule.INSTANCE);
        hud.addModule(ClickGuiModule.INSTANCE);
        categories.add(hud);

        // UI Category
        ModuleCategory ui = new ModuleCategory("UI");
        ui.addModule(UiUtilitiesModule.INSTANCE);
        categories.add(ui);

        // Utilities Category
        ModuleCategory utilities = new ModuleCategory("Utilities");
        utilities.addModule(XrayModule.INSTANCE);
        utilities.addModule(CommandHistoryModule.INSTANCE);
        utilities.addModule(InstaBreakModule.INSTANCE);
        utilities.addModule(NBTTooltipModule.INSTANCE);
        utilities.addModule(FullbrightModule.INSTANCE);
        categories.add(utilities);

        // Movement Category
        ModuleCategory movement = new ModuleCategory("Movement");
        movement.addModule(InventoryMoveModule.INSTANCE);
        movement.addModule(ZoomModule.INSTANCE);
        movement.addModule(WaypointModule.INSTANCE);
        movement.addModule(InventoryMoveModule.INSTANCE);
        movement.addModule(FreecamModule.INSTANCE);
        categories.add(movement);

        // Chat Category
        ModuleCategory chat = new ModuleCategory("Chat");
        chat.addModule(SecondaryChatModule.INSTANCE);
        categories.add(chat);
    }

    public static ModuleCategory[] getCategories() {
        return categories.toArray(new ModuleCategory[0]);
    }

}

