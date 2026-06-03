package me.m0dii.modules.macros.gui;

import me.m0dii.modules.chat.SecondaryChatModule;
import me.m0dii.modules.chat.SecondaryChatSettings;
import me.m0dii.modules.commandblockui.BetterCommandBlockUiModule;
import me.m0dii.modules.fastblockplacement.FastBlockPlacementModule;
import me.m0dii.modules.freecam.FreecamModule;
import me.m0dii.modules.fullbright.FullbrightModule;
import me.m0dii.modules.heldlight.HeldLightModule;
import me.m0dii.modules.hungertweaks.HungerTweaksModule;
import me.m0dii.modules.instantbreak.InstantBreakModule;
import me.m0dii.modules.inventorymove.InventoryMoveModule;
import me.m0dii.modules.mousetweaks.MouseTweaksModule;
import me.m0dii.modules.mousetweaks.MouseTweaksScrollItemScaling;
import me.m0dii.modules.mousetweaks.MouseTweaksWheelScrollDirection;
import me.m0dii.modules.mousetweaks.MouseTweaksWheelSearchOrder;
import me.m0dii.modules.nbthud.NBTInfoHudModule;
import me.m0dii.modules.nbttooltip.NBTTooltipModule;
import me.m0dii.modules.nbttooltip.ShulkerTooltipModule;
import me.m0dii.modules.overlays.*;
import me.m0dii.modules.pickup.ItemPickupNotifierModule;
import me.m0dii.modules.pickup.PickupFeedSettings;
import me.m0dii.modules.reach.ReachModule;
import me.m0dii.modules.tweaks.TweaksModule;
import me.m0dii.modules.waypoints.WaypointModule;
import me.m0dii.utils.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;

final class MacroWorkbenchConfigurationTab {
    private static final int TOP_BAR_H = 54;
    private static final int ROW_H = 20;
    private static final int ROW_GAP = 24;
    private static final int CONTENT_START_Y = TOP_BAR_H + 34;

    enum Category {
        HUD("HUD"),
        OVERLAYS("Overlays"),
        MODULES("Modules"),
        SECONDARY_CHAT("Secondary Chat"),
        PICKUP_FEED("Pickup Feed"),
        BLOCK_ATTRIBUTES("Block Attributes"),
        TWEAKS("Tweaks"),
        MOUSE_TWEAKS("Mouse Tweaks"),
        HUNGER_TWEAKS("Hunger Tweaks");

        private final String label;

        Category(String label) {
            this.label = label;
        }
    }

    private final MacroWorkbenchScreen owner;
    private final List<ClickableWidget> configWidgets;
    private final BooleanSupplier shiftDown;

    private Category category = Category.HUD;

    private ButtonWidget hudCategoryButton;
    private ButtonWidget overlaysCategoryButton;
    private ButtonWidget modulesCategoryButton;
    private ButtonWidget secondaryChatCategoryButton;
    private ButtonWidget pickupFeedCategoryButton;
    private ButtonWidget blockAttributesCategoryButton;
    private ButtonWidget tweaksCategoryButton;
    private ButtonWidget mouseTweaksCategoryButton;
    private ButtonWidget hungerTweaksCategoryButton;

    private ButtonWidget macroOverlayToggleButton;
    private ButtonWidget nbtHudToggleButton;

    private ButtonWidget secondaryEnabledToggleButton;
    private ButtonWidget secondaryOverlayToggleButton;
    private ButtonWidget secondaryInterceptModeButton;
    private ButtonWidget secondaryRegexAddButton;
    private ButtonWidget secondaryRegexApplyButton;
    private ButtonWidget secondaryRegexRemoveButton;
    private ButtonWidget secondaryRegexClearButton;
    private ButtonWidget secondaryOutgoingApplyButton;

    private ButtonWidget pickupEnabledButton;
    private ButtonWidget pickupDurationButton;
    private ButtonWidget pickupLinesButton;
    private ButtonWidget pickupIconScaleButton;
    private ButtonWidget pickupDirectionButton;

    private ButtonWidget freecamToggleButton;
    private ButtonWidget fullbrightToggleButton;
    private ButtonWidget heldLightToggleButton;
    private ButtonWidget inventoryMoveToggleButton;
    private ButtonWidget instantBreakToggleButton;
    private ButtonWidget fastBlockPlacementToggleButton;
    private ButtonWidget betterCommandBlockUiToggleButton;
    private ButtonWidget waypointsToggleButton;
    private ButtonWidget nbtTooltipToggleButton;
    private ButtonWidget shulkerTooltipToggleButton;

    private ButtonWidget biomeBorderToggleButton;
    private ButtonWidget chunkBorderToggleButton;
    private ButtonWidget slimeChunksToggleButton;
    private ButtonWidget structureBoundsToggleButton;
    private ButtonWidget commandBlockOverlayToggleButton;
    private ButtonWidget lightOverlayToggleButton;

    private ButtonWidget collisionMeshToggleButton;
    private ButtonWidget lightBlocksToggleButton;
    private ButtonWidget preventInteractionsToggleButton;
    private ButtonWidget solidFluidHitboxesToggleButton;
    private ButtonWidget barrierBlocksToggleButton;

    private ButtonWidget tweaksModuleToggleButton;
    private ButtonWidget hideOwnEffectsToggleButton;
    private ButtonWidget hideOffhandItemToggleButton;
    private ButtonWidget disableBlockBreakParticlesToggleButton;
    private ButtonWidget disableEntityRenderingToggleButton;
    private ButtonWidget disableNetherFogToggleButton;
    private ButtonWidget disableRenderDistanceFogToggleButton;
    private ButtonWidget disableRainEffectsToggleButton;
    private ButtonWidget disableSoundsToggleButton;
    private ButtonWidget disableWallUnsprintToggleButton;
    private ButtonWidget angelBlockToggleButton;
    private ButtonWidget permanentSneakToggleButton;
    private ButtonWidget permanentSprintToggleButton;
    private ButtonWidget disableHurtCameraToggleButton;
    private ButtonWidget disableViewBobbingToggleButton;
    private ButtonWidget reachToggleButton;
    private ButtonWidget reachSafeClampToggleButton;
    private ButtonWidget reachBlockDistanceButton;
    private ButtonWidget reachEntityDistanceButton;
    private ButtonWidget reachMpBlockExtraButton;
    private ButtonWidget reachMpEntityExtraButton;
    private ButtonWidget mouseTweaksModuleToggleButton;
    private ButtonWidget mouseTweaksRmbToggleButton;
    private ButtonWidget mouseTweaksLmbWithItemToggleButton;
    private ButtonWidget mouseTweaksLmbWithoutItemToggleButton;
    private ButtonWidget mouseTweaksWheelToggleButton;
    private ButtonWidget mouseTweaksWheelSearchOrderButton;
    private ButtonWidget mouseTweaksWheelScrollDirectionButton;
    private ButtonWidget mouseTweaksScrollScalingButton;
    private ButtonWidget hungerTweaksModuleToggleButton;
    private ButtonWidget hungerTooltipToggleButton;
    private ButtonWidget hungerTooltipAlwaysToggleButton;
    private ButtonWidget hungerSaturationOverlayToggleButton;
    private ButtonWidget hungerFoodValuesOverlayToggleButton;
    private ButtonWidget hungerOffhandOverlayToggleButton;
    private ButtonWidget hungerExhaustionUnderlayToggleButton;
    private ButtonWidget hungerHealthOverlayToggleButton;
    private ButtonWidget hungerDebugInfoToggleButton;
    private ButtonWidget hungerVanillaAnimationToggleButton;
    private ButtonWidget hungerMaxFlashAlphaButton;

    private TextFieldWidget secondaryRegexInputField;
    private TextFieldWidget secondaryOutgoingRegexField;

    private int selectedRegexIndex = -1;
    private int regexScroll = 0;

    MacroWorkbenchConfigurationTab(MacroWorkbenchScreen owner,
                                   List<ClickableWidget> configWidgets,
                                   BooleanSupplier shiftDown) {
        this.owner = owner;
        this.configWidgets = configWidgets;
        this.shiftDown = shiftDown;
    }

    void initWidgets() {
        int listX = 12;
        int listY = TOP_BAR_H + 18;
        int categoryW = Math.max(130, (this.owner.width / 2) - 28);
        int rightX = (this.owner.width / 2) + 12;
        int settingW = Math.max(180, this.owner.width - rightX - 12);

        this.hudCategoryButton = button(Category.HUD.label, b -> setCategory(Category.HUD), listX, listY, categoryW, ROW_H);
        this.overlaysCategoryButton = button(Category.OVERLAYS.label, b -> setCategory(Category.OVERLAYS), listX, listY + ROW_GAP, categoryW, ROW_H);
        this.modulesCategoryButton = button(Category.MODULES.label, b -> setCategory(Category.MODULES), listX, listY + ROW_GAP * 2, categoryW, ROW_H);
        this.secondaryChatCategoryButton = button(Category.SECONDARY_CHAT.label, b -> setCategory(Category.SECONDARY_CHAT), listX, listY + ROW_GAP * 3, categoryW, ROW_H);
        this.pickupFeedCategoryButton = button(Category.PICKUP_FEED.label, b -> setCategory(Category.PICKUP_FEED), listX, listY + ROW_GAP * 4, categoryW, ROW_H);
        this.blockAttributesCategoryButton = button(Category.BLOCK_ATTRIBUTES.label, b -> setCategory(Category.BLOCK_ATTRIBUTES), listX, listY + ROW_GAP * 5, categoryW, ROW_H);
        this.tweaksCategoryButton = button(Category.TWEAKS.label, b -> setCategory(Category.TWEAKS), listX, listY + ROW_GAP * 6, categoryW, ROW_H);
        this.mouseTweaksCategoryButton = button(Category.MOUSE_TWEAKS.label, b -> setCategory(Category.MOUSE_TWEAKS), listX, listY + ROW_GAP * 7, categoryW, ROW_H);
        this.hungerTweaksCategoryButton = button(Category.HUNGER_TWEAKS.label, b -> setCategory(Category.HUNGER_TWEAKS), listX, listY + ROW_GAP * 8, categoryW, ROW_H);

        this.macroOverlayToggleButton = button("Macro Keybind HUD", b -> {
            ModConfig.updateAndSave(() -> ModConfig.showMacroKeybindOverlay = !ModConfig.showMacroKeybindOverlay);
            syncControls();
        }, rightX, rowY(0), settingW, ROW_H);

        this.nbtHudToggleButton = button("NBT Inspector HUD", b -> {
            NBTInfoHudModule.INSTANCE.setEnabled(!NBTInfoHudModule.INSTANCE.isEnabled());
            syncControls();
        }, rightX, rowY(1), settingW, ROW_H);

        this.secondaryEnabledToggleButton = button("Secondary Chat", b -> {
            SecondaryChatModule.INSTANCE.setEnabled(!SecondaryChatModule.INSTANCE.isEnabled());
            syncControls();
        }, rightX, rowY(0), settingW, ROW_H);

        this.secondaryOverlayToggleButton = button("Secondary Chat Overlay", b -> {
            SecondaryChatSettings.updateAndSave(() -> SecondaryChatSettings.get().showOverlay = !SecondaryChatSettings.get().showOverlay);
            syncControls();
        }, rightX, rowY(1), settingW, ROW_H);

        this.secondaryInterceptModeButton = button("Intercept Mode", b -> {
            SecondaryChatSettings.updateAndSave(() -> SecondaryChatSettings.get().interceptMode =
                    SecondaryChatSettings.get().interceptMode == SecondaryChatSettings.InterceptMode.COPY
                            ? SecondaryChatSettings.InterceptMode.MOVE
                            : SecondaryChatSettings.InterceptMode.COPY);
            syncControls();
        }, rightX, rowY(2), settingW, ROW_H);

        this.secondaryRegexInputField = new TextFieldWidget(this.owner.workbenchTextRenderer(), rightX, rowY(3), settingW - 114, ROW_H, Text.literal("Regex"));
        this.secondaryRegexInputField.setMaxLength(180);

        this.secondaryRegexAddButton = button("Add", b -> {
            String regex = safeField(this.secondaryRegexInputField);
            if (!regex.isEmpty()) {
                SecondaryChatSettings.updateAndSave(() -> SecondaryChatSettings.get().regexList.add(regex));
                this.secondaryRegexInputField.setText("");
                this.selectedRegexIndex = SecondaryChatSettings.get().regexList.size() - 1;
                ensureRegexSelectionInBounds();
                syncControls();
            }
        }, rightX + settingW - 110, rowY(3), 110, ROW_H);

        this.secondaryRegexApplyButton = button("Apply Selected", b -> {
            String regex = safeField(this.secondaryRegexInputField);
            if (regex.isEmpty()) {
                return;
            }
            SecondaryChatSettings.updateAndSave(() -> {
                List<String> list = SecondaryChatSettings.get().regexList;
                if (list != null && this.selectedRegexIndex >= 0 && this.selectedRegexIndex < list.size()) {
                    list.set(this.selectedRegexIndex, regex);
                }
            });
            syncControls();
        }, rightX, rowY(9), 140, ROW_H);

        this.secondaryRegexRemoveButton = button("Remove Selected", b -> {
            SecondaryChatSettings.updateAndSave(() -> {
                List<String> list = SecondaryChatSettings.get().regexList;
                if (list != null && this.selectedRegexIndex >= 0 && this.selectedRegexIndex < list.size()) {
                    list.remove(this.selectedRegexIndex);
                }
            });
            if (this.selectedRegexIndex > 0) {
                this.selectedRegexIndex--;
            }
            ensureRegexSelectionInBounds();
            syncControls();
        }, rightX + 144, rowY(9), 140, ROW_H);

        this.secondaryRegexClearButton = button("Clear All", b -> {
            SecondaryChatSettings.updateAndSave(() -> SecondaryChatSettings.get().regexList = new ArrayList<>());
            this.selectedRegexIndex = -1;
            this.regexScroll = 0;
            syncControls();
        }, rightX + 288, rowY(9), 100, ROW_H);

        this.secondaryOutgoingRegexField = new TextFieldWidget(this.owner.workbenchTextRenderer(), rightX, rowY(10), settingW - 110, ROW_H, Text.literal("Outgoing regex"));
        this.secondaryOutgoingRegexField.setMaxLength(180);

        this.secondaryOutgoingApplyButton = button("Apply", b -> {
            String outgoing = safeField(this.secondaryOutgoingRegexField);
            SecondaryChatSettings.updateAndSave(() -> SecondaryChatSettings.get().outgoingRegex = outgoing);
            syncControls();
        }, rightX + settingW - 106, rowY(10), 106, ROW_H);

        this.pickupEnabledButton = button("Pickup Notifier", b -> {
            ItemPickupNotifierModule.INSTANCE.setEnabled(!ItemPickupNotifierModule.INSTANCE.isEnabled());
            syncControls();
        }, rightX, rowY(0), settingW, ROW_H);

        this.pickupDurationButton = button("Pickup Feed Duration", b -> {
            adjustPickupDuration(1);
            syncControls();
        }, rightX, rowY(1), settingW, ROW_H);

        this.pickupLinesButton = button("Pickup Feed Max Lines", b -> {
            adjustPickupLines(1);
            syncControls();
        }, rightX, rowY(2), settingW, ROW_H);

        this.pickupIconScaleButton = button("Pickup Feed Icon Scale", b -> {
            adjustPickupIconScale(1);
            syncControls();
        }, rightX, rowY(3), settingW, ROW_H);

        this.pickupDirectionButton = button("Pickup Feed Direction", b -> {
            cyclePickupDirection(true);
            syncControls();
        }, rightX, rowY(4), settingW, ROW_H);

        this.freecamToggleButton = button("Freecam", b -> {
            FreecamModule.INSTANCE.setEnabled(!FreecamModule.INSTANCE.isEnabled());
            syncControls();
        }, rightX, rowY(0), settingW, ROW_H);

        this.fullbrightToggleButton = button("Fullbright", b -> {
            FullbrightModule.INSTANCE.setEnabled(!FullbrightModule.INSTANCE.isEnabled());
            syncControls();
        }, rightX, rowY(1), settingW, ROW_H);

        this.heldLightToggleButton = button("Held Light", b -> {
            HeldLightModule.INSTANCE.setEnabled(!HeldLightModule.INSTANCE.isEnabled());
            syncControls();
        }, rightX, rowY(2), settingW, ROW_H);

        this.inventoryMoveToggleButton = button("Inventory Move", b -> {
            InventoryMoveModule.INSTANCE.setEnabled(!InventoryMoveModule.INSTANCE.isEnabled());
            syncControls();
        }, rightX, rowY(3), settingW, ROW_H);

        this.instantBreakToggleButton = button("Instant Break", b -> {
            InstantBreakModule.INSTANCE.setEnabled(!InstantBreakModule.INSTANCE.isEnabled());
            syncControls();
        }, rightX, rowY(4), settingW, ROW_H);

        this.fastBlockPlacementToggleButton = button("Fast Place", b -> {
            FastBlockPlacementModule.INSTANCE.setEnabled(!FastBlockPlacementModule.INSTANCE.isEnabled());
            syncControls();
        }, rightX, rowY(5), settingW, ROW_H);

        this.betterCommandBlockUiToggleButton = button("Better Command Block UI", b -> {
            BetterCommandBlockUiModule.INSTANCE.setEnabled(!BetterCommandBlockUiModule.INSTANCE.isEnabled());
            syncControls();
        }, rightX, rowY(6), settingW, ROW_H);

        this.waypointsToggleButton = button("Waypoints", b -> {
            WaypointModule.INSTANCE.setEnabled(!WaypointModule.INSTANCE.isEnabled());
            syncControls();
        }, rightX, rowY(7), settingW, ROW_H);

        this.nbtTooltipToggleButton = button("NBT Tooltip", b -> {
            NBTTooltipModule.INSTANCE.setEnabled(!NBTTooltipModule.INSTANCE.isEnabled());
            syncControls();
        }, rightX, rowY(8), settingW, ROW_H);

        this.shulkerTooltipToggleButton = button("Shulker Preview Tooltip", b -> {
            ShulkerTooltipModule.INSTANCE.setEnabled(!ShulkerTooltipModule.INSTANCE.isEnabled());
            syncControls();
        }, rightX, rowY(9), settingW, ROW_H);

        this.biomeBorderToggleButton = button("Biome Border Overlay", b -> {
            BiomeBorderOverlayModule.INSTANCE.setEnabled(!BiomeBorderOverlayModule.INSTANCE.isEnabled());
            syncControls();
        }, rightX, rowY(0), settingW, ROW_H);

        this.chunkBorderToggleButton = button("Chunk Border Overlay", b -> {
            ChunkBorderOverlayModule.INSTANCE.setEnabled(!ChunkBorderOverlayModule.INSTANCE.isEnabled());
            syncControls();
        }, rightX, rowY(1), settingW, ROW_H);

        this.slimeChunksToggleButton = button("Slime Chunk Overlay", b -> {
            SlimeChunkOverlayModule.INSTANCE.setEnabled(!SlimeChunkOverlayModule.INSTANCE.isEnabled());
            syncControls();
        }, rightX, rowY(2), settingW, ROW_H);

        this.structureBoundsToggleButton = button("Structure Bounding Boxes", b -> {
            StructureBoundingBoxOverlay.INSTANCE.setEnabled(!StructureBoundingBoxOverlay.INSTANCE.isEnabled());
            syncControls();
        }, rightX, rowY(3), settingW, ROW_H);

        this.commandBlockOverlayToggleButton = button("Command Block Overlay", b -> {
            CommandBlockOverlayModule.INSTANCE.setEnabled(!CommandBlockOverlayModule.INSTANCE.isEnabled());
            syncControls();
        }, rightX, rowY(4), settingW, ROW_H);

        this.lightOverlayToggleButton = button("Light Level Overlay", b -> {
            LightLevelOverlayModule.INSTANCE.setEnabled(!LightLevelOverlayModule.INSTANCE.isEnabled());
            syncControls();
        }, rightX, rowY(5), settingW, ROW_H);

        this.collisionMeshToggleButton = button("Show Collision Mesh", b -> {
            ModConfig.updateAndSave(() -> ModConfig.blockAttributesShowCollisionMesh = !ModConfig.blockAttributesShowCollisionMesh);
            syncControls();
        }, rightX, rowY(0), settingW, ROW_H);

        this.lightBlocksToggleButton = button("Show Light Blocks", b -> {
            ModConfig.updateAndSave(() -> ModConfig.blockAttributesShowLightBlocks = !ModConfig.blockAttributesShowLightBlocks);
            syncControls();
        }, rightX, rowY(1), settingW, ROW_H);

        this.preventInteractionsToggleButton = button("Prevent Interactions", b -> {
            ModConfig.updateAndSave(() -> ModConfig.blockAttributesPreventInteractions = !ModConfig.blockAttributesPreventInteractions);
            syncControls();
        }, rightX, rowY(2), settingW, ROW_H);

        this.solidFluidHitboxesToggleButton = button("Solid Fluid Hitboxes", b -> {
            ModConfig.updateAndSave(() -> ModConfig.blockAttributesSolidFluidHitboxes = !ModConfig.blockAttributesSolidFluidHitboxes);
            syncControls();
        }, rightX, rowY(3), settingW, ROW_H);

        this.barrierBlocksToggleButton = button("Show Barrier Blocks", b -> {
            ModConfig.updateAndSave(() -> ModConfig.blockAttributesShowBarrierBlocks = !ModConfig.blockAttributesShowBarrierBlocks);
            syncControls();
        }, rightX, rowY(4), settingW, ROW_H);

        this.tweaksModuleToggleButton = button("Tweaks Module", b -> {
            TweaksModule.INSTANCE.setEnabled(!TweaksModule.INSTANCE.isEnabled());
            syncControls();
        }, rightX, rowY(0), settingW, ROW_H);

        this.hideOwnEffectsToggleButton = button("Hide Own Effect Particles", b -> {
            ModConfig.updateAndSave(() -> ModConfig.tweaksHideOwnEffectParticles = !ModConfig.tweaksHideOwnEffectParticles);
            syncControls();
        }, rightX, rowY(1), settingW, ROW_H);

        this.hideOffhandItemToggleButton = button("Hide Offhand Item", b -> {
            ModConfig.updateAndSave(() -> ModConfig.tweaksHideOffhandItem = !ModConfig.tweaksHideOffhandItem);
            syncControls();
        }, rightX, rowY(2), settingW, ROW_H);

        this.disableBlockBreakParticlesToggleButton = button("Disable Block Breaking Particles", b -> {
            ModConfig.updateAndSave(() -> ModConfig.tweaksDisableBlockBreakingParticles = !ModConfig.tweaksDisableBlockBreakingParticles);
            syncControls();
        }, rightX, rowY(3), settingW, ROW_H);

        this.disableEntityRenderingToggleButton = button("Disable Entity Rendering", b -> {
            ModConfig.updateAndSave(() -> ModConfig.tweaksDisableEntityRendering = !ModConfig.tweaksDisableEntityRendering);
            syncControls();
        }, rightX, rowY(4), settingW, ROW_H);

        this.disableNetherFogToggleButton = button("Disable Nether Fog", b -> {
            ModConfig.updateAndSave(() -> ModConfig.tweaksDisableNetherFog = !ModConfig.tweaksDisableNetherFog);
            syncControls();
        }, rightX, rowY(5), settingW, ROW_H);

        this.disableRenderDistanceFogToggleButton = button("Disable Render-Distance Fog", b -> {
            ModConfig.updateAndSave(() -> ModConfig.tweaksDisableRenderDistanceFog = !ModConfig.tweaksDisableRenderDistanceFog);
            syncControls();
        }, rightX, rowY(6), settingW, ROW_H);

        this.disableRainEffectsToggleButton = button("Disable Rain Effects", b -> {
            ModConfig.updateAndSave(() -> ModConfig.tweaksDisableRainEffects = !ModConfig.tweaksDisableRainEffects);
            syncControls();
        }, rightX, rowY(7), settingW, ROW_H);

        this.disableSoundsToggleButton = button("Disable Sounds", b -> {
            ModConfig.updateAndSave(() -> ModConfig.tweaksDisableSounds = !ModConfig.tweaksDisableSounds);
            syncControls();
        }, rightX, rowY(8), settingW, ROW_H);

        this.disableWallUnsprintToggleButton = button("Disable Wall Unsprint", b -> {
            ModConfig.updateAndSave(() -> ModConfig.tweaksDisableWallUnsprint = !ModConfig.tweaksDisableWallUnsprint);
            syncControls();
        }, rightX, rowY(9), settingW, ROW_H);

        this.angelBlockToggleButton = button("Angel Block", b -> {
            ModConfig.updateAndSave(() -> ModConfig.tweaksAngelBlock = !ModConfig.tweaksAngelBlock);
            syncControls();
        }, rightX, rowY(10), settingW, ROW_H);

        this.permanentSneakToggleButton = button("Permanent Sneak", b -> {
            ModConfig.updateAndSave(() -> ModConfig.tweaksPermanentSneak = !ModConfig.tweaksPermanentSneak);
            syncControls();
        }, rightX, rowY(11), settingW, ROW_H);

        this.permanentSprintToggleButton = button("Permanent Sprint", b -> {
            ModConfig.updateAndSave(() -> ModConfig.tweaksPermanentSprint = !ModConfig.tweaksPermanentSprint);
            syncControls();
        }, rightX, rowY(12), settingW, ROW_H);

        this.disableHurtCameraToggleButton = button("Disable Hurt Camera", b -> {
            ModConfig.updateAndSave(() -> ModConfig.tweaksDisableHurtCamera = !ModConfig.tweaksDisableHurtCamera);
            syncControls();
        }, rightX, rowY(13), settingW, ROW_H);

        this.disableViewBobbingToggleButton = button("Disable View Bobbing", b -> {
            ModConfig.updateAndSave(() -> ModConfig.tweaksDisableViewBobbing = !ModConfig.tweaksDisableViewBobbing);
            syncControls();
        }, rightX, rowY(14), settingW, ROW_H);

        this.reachToggleButton = button("Reach", b -> {
            ReachModule.INSTANCE.setEnabled(!ReachModule.INSTANCE.isEnabled());
            syncControls();
        }, rightX, rowY(15), settingW, ROW_H);

        this.reachSafeClampToggleButton = button("Reach Safe Multiplayer Clamp", b -> {
            ModConfig.updateAndSave(() -> ModConfig.reachSafeMultiplayerClamp = !ModConfig.reachSafeMultiplayerClamp);
            syncControls();
        }, rightX, rowY(16), settingW, ROW_H);

        this.reachBlockDistanceButton = button("Reach Block Distance", b -> {
            adjustReachBlockDistance(1);
            syncControls();
        }, rightX, rowY(17), settingW, ROW_H);

        this.reachEntityDistanceButton = button("Reach Entity Distance", b -> {
            adjustReachEntityDistance(1);
            syncControls();
        }, rightX, rowY(18), settingW, ROW_H);

        this.reachMpBlockExtraButton = button("Reach MP Block Extra", b -> {
            adjustReachMpBlockExtra(1);
            syncControls();
        }, rightX, rowY(19), settingW, ROW_H);

        this.reachMpEntityExtraButton = button("Reach MP Entity Extra", b -> {
            adjustReachMpEntityExtra(1);
            syncControls();
        }, rightX, rowY(20), settingW, ROW_H);

        this.mouseTweaksModuleToggleButton = button("Mouse Tweaks Module", b -> {
            MouseTweaksModule.INSTANCE.setEnabled(!MouseTweaksModule.INSTANCE.isEnabled());
            syncControls();
        }, rightX, rowY(0), settingW, ROW_H);

        this.mouseTweaksRmbToggleButton = button("RMB Tweak", b -> {
            ModConfig.updateAndSave(() -> ModConfig.mouseTweaksRmbTweak = !ModConfig.mouseTweaksRmbTweak);
            syncControls();
        }, rightX, rowY(1), settingW, ROW_H);

        this.mouseTweaksLmbWithItemToggleButton = button("LMB Tweak With Item", b -> {
            ModConfig.updateAndSave(() -> ModConfig.mouseTweaksLmbTweakWithItem = !ModConfig.mouseTweaksLmbTweakWithItem);
            syncControls();
        }, rightX, rowY(2), settingW, ROW_H);

        this.mouseTweaksLmbWithoutItemToggleButton = button("LMB Tweak Without Item", b -> {
            ModConfig.updateAndSave(() -> ModConfig.mouseTweaksLmbTweakWithoutItem = !ModConfig.mouseTweaksLmbTweakWithoutItem);
            syncControls();
        }, rightX, rowY(3), settingW, ROW_H);

        this.mouseTweaksWheelToggleButton = button("Wheel Tweak", b -> {
            ModConfig.updateAndSave(() -> ModConfig.mouseTweaksWheelTweak = !ModConfig.mouseTweaksWheelTweak);
            syncControls();
        }, rightX, rowY(4), settingW, ROW_H);

        this.mouseTweaksWheelSearchOrderButton = button("Wheel Search Order", b -> {
            ModConfig.updateAndSave(() -> ModConfig.mouseTweaksWheelSearchOrder = nextWheelSearchOrder(ModConfig.mouseTweaksWheelSearchOrder));
            syncControls();
        }, rightX, rowY(5), settingW, ROW_H);

        this.mouseTweaksWheelScrollDirectionButton = button("Wheel Scroll Direction", b -> {
            ModConfig.updateAndSave(() -> ModConfig.mouseTweaksWheelScrollDirection = nextWheelScrollDirection(ModConfig.mouseTweaksWheelScrollDirection));
            syncControls();
        }, rightX, rowY(6), settingW, ROW_H);

        this.mouseTweaksScrollScalingButton = button("Scroll Item Scaling", b -> {
            ModConfig.updateAndSave(() -> ModConfig.mouseTweaksScrollItemScaling = nextScrollScaling(ModConfig.mouseTweaksScrollItemScaling));
            syncControls();
        }, rightX, rowY(7), settingW, ROW_H);

        this.hungerTweaksModuleToggleButton = button("Hunger Tweaks Module", b -> {
            HungerTweaksModule.INSTANCE.setEnabled(!HungerTweaksModule.INSTANCE.isEnabled());
            syncControls();
        }, rightX, rowY(0), settingW, ROW_H);

        this.hungerTooltipToggleButton = button("Tooltip Food Values", b -> {
            ModConfig.updateAndSave(() -> ModConfig.hungerTweaksShowFoodValuesInTooltip = !ModConfig.hungerTweaksShowFoodValuesInTooltip);
            syncControls();
        }, rightX, rowY(1), settingW, ROW_H);

        this.hungerTooltipAlwaysToggleButton = button("Tooltip Always Visible", b -> {
            ModConfig.updateAndSave(() -> ModConfig.hungerTweaksShowFoodValuesInTooltipAlways = !ModConfig.hungerTweaksShowFoodValuesInTooltipAlways);
            syncControls();
        }, rightX, rowY(2), settingW, ROW_H);

        this.hungerSaturationOverlayToggleButton = button("Saturation Overlay", b -> {
            ModConfig.updateAndSave(() -> ModConfig.hungerTweaksShowSaturationHudOverlay = !ModConfig.hungerTweaksShowSaturationHudOverlay);
            syncControls();
        }, rightX, rowY(3), settingW, ROW_H);

        this.hungerFoodValuesOverlayToggleButton = button("Held Food Overlay", b -> {
            ModConfig.updateAndSave(() -> ModConfig.hungerTweaksShowFoodValuesHudOverlay = !ModConfig.hungerTweaksShowFoodValuesHudOverlay);
            syncControls();
        }, rightX, rowY(4), settingW, ROW_H);

        this.hungerOffhandOverlayToggleButton = button("Offhand Overlay", b -> {
            ModConfig.updateAndSave(() -> ModConfig.hungerTweaksShowFoodValuesHudOverlayWhenOffhand = !ModConfig.hungerTweaksShowFoodValuesHudOverlayWhenOffhand);
            syncControls();
        }, rightX, rowY(5), settingW, ROW_H);

        this.hungerExhaustionUnderlayToggleButton = button("Exhaustion Underlay", b -> {
            ModConfig.updateAndSave(() -> ModConfig.hungerTweaksShowFoodExhaustionHudUnderlay = !ModConfig.hungerTweaksShowFoodExhaustionHudUnderlay);
            syncControls();
        }, rightX, rowY(6), settingW, ROW_H);

        this.hungerHealthOverlayToggleButton = button("Estimated Health Overlay", b -> {
            ModConfig.updateAndSave(() -> ModConfig.hungerTweaksShowFoodHealthHudOverlay = !ModConfig.hungerTweaksShowFoodHealthHudOverlay);
            syncControls();
        }, rightX, rowY(7), settingW, ROW_H);

        this.hungerDebugInfoToggleButton = button("Debug HUD Food Info", b -> {
            ModConfig.updateAndSave(() -> ModConfig.hungerTweaksShowFoodDebugInfo = !ModConfig.hungerTweaksShowFoodDebugInfo);
            syncControls();
        }, rightX, rowY(8), settingW, ROW_H);

        this.hungerVanillaAnimationToggleButton = button("Match Vanilla Animation", b -> {
            ModConfig.updateAndSave(() -> ModConfig.hungerTweaksShowVanillaAnimationsOverlay = !ModConfig.hungerTweaksShowVanillaAnimationsOverlay);
            syncControls();
        }, rightX, rowY(9), settingW, ROW_H);

        this.hungerMaxFlashAlphaButton = button("Max Flash Alpha", b -> {
            adjustHungerMaxFlashAlpha(1);
            syncControls();
        }, rightX, rowY(10), settingW, ROW_H);

        register(
                this.hudCategoryButton, this.overlaysCategoryButton, this.modulesCategoryButton,
                this.secondaryChatCategoryButton, this.pickupFeedCategoryButton, this.blockAttributesCategoryButton,
                this.tweaksCategoryButton, this.mouseTweaksCategoryButton, this.hungerTweaksCategoryButton,
                this.macroOverlayToggleButton, this.nbtHudToggleButton,
                this.secondaryEnabledToggleButton, this.secondaryOverlayToggleButton, this.secondaryInterceptModeButton,
                this.secondaryRegexAddButton, this.secondaryRegexApplyButton, this.secondaryRegexRemoveButton,
                this.secondaryRegexClearButton, this.secondaryOutgoingApplyButton,
                this.pickupEnabledButton, this.pickupDurationButton, this.pickupLinesButton,
                this.pickupIconScaleButton, this.pickupDirectionButton,
                this.freecamToggleButton, this.fullbrightToggleButton, this.heldLightToggleButton,
                this.inventoryMoveToggleButton, this.instantBreakToggleButton, this.fastBlockPlacementToggleButton,
                this.betterCommandBlockUiToggleButton, this.waypointsToggleButton,
                this.nbtTooltipToggleButton, this.shulkerTooltipToggleButton,
                this.biomeBorderToggleButton, this.chunkBorderToggleButton, this.slimeChunksToggleButton,
                this.structureBoundsToggleButton, this.commandBlockOverlayToggleButton, this.lightOverlayToggleButton,
                this.collisionMeshToggleButton, this.lightBlocksToggleButton, this.preventInteractionsToggleButton,
                this.solidFluidHitboxesToggleButton, this.barrierBlocksToggleButton,
                this.tweaksModuleToggleButton, this.hideOwnEffectsToggleButton, this.hideOffhandItemToggleButton,
                this.disableBlockBreakParticlesToggleButton, this.disableEntityRenderingToggleButton,
                this.disableNetherFogToggleButton, this.disableRenderDistanceFogToggleButton, this.disableRainEffectsToggleButton,
                this.disableSoundsToggleButton, this.disableWallUnsprintToggleButton, this.angelBlockToggleButton,
                this.permanentSneakToggleButton, this.permanentSprintToggleButton, this.disableHurtCameraToggleButton,
                this.disableViewBobbingToggleButton, this.reachToggleButton, this.reachSafeClampToggleButton,
                this.reachBlockDistanceButton, this.reachEntityDistanceButton, this.reachMpBlockExtraButton, this.reachMpEntityExtraButton,
                this.mouseTweaksModuleToggleButton, this.mouseTweaksRmbToggleButton, this.mouseTweaksLmbWithItemToggleButton,
                this.mouseTweaksLmbWithoutItemToggleButton, this.mouseTweaksWheelToggleButton, this.mouseTweaksWheelSearchOrderButton,
                this.mouseTweaksWheelScrollDirectionButton, this.mouseTweaksScrollScalingButton,
                this.hungerTweaksModuleToggleButton, this.hungerTooltipToggleButton, this.hungerTooltipAlwaysToggleButton,
                this.hungerSaturationOverlayToggleButton, this.hungerFoodValuesOverlayToggleButton, this.hungerOffhandOverlayToggleButton,
                this.hungerExhaustionUnderlayToggleButton, this.hungerHealthOverlayToggleButton, this.hungerDebugInfoToggleButton,
                this.hungerVanillaAnimationToggleButton, this.hungerMaxFlashAlphaButton,
                this.secondaryRegexInputField, this.secondaryOutgoingRegexField
        );

        syncControls();
    }

    void syncControls() {
        if (this.hudCategoryButton == null) {
            return;
        }

        this.hudCategoryButton.setMessage(Text.literal((this.category == Category.HUD ? "> " : "") + Category.HUD.label));
        this.overlaysCategoryButton.setMessage(Text.literal((this.category == Category.OVERLAYS ? "> " : "") + Category.OVERLAYS.label));
        this.modulesCategoryButton.setMessage(Text.literal((this.category == Category.MODULES ? "> " : "") + Category.MODULES.label));
        this.secondaryChatCategoryButton.setMessage(Text.literal((this.category == Category.SECONDARY_CHAT ? "> " : "") + Category.SECONDARY_CHAT.label));
        this.pickupFeedCategoryButton.setMessage(Text.literal((this.category == Category.PICKUP_FEED ? "> " : "") + Category.PICKUP_FEED.label));
        this.blockAttributesCategoryButton.setMessage(Text.literal((this.category == Category.BLOCK_ATTRIBUTES ? "> " : "") + Category.BLOCK_ATTRIBUTES.label));
        this.tweaksCategoryButton.setMessage(Text.literal((this.category == Category.TWEAKS ? "> " : "") + Category.TWEAKS.label));
        this.mouseTweaksCategoryButton.setMessage(Text.literal((this.category == Category.MOUSE_TWEAKS ? "> " : "") + Category.MOUSE_TWEAKS.label));
        this.hungerTweaksCategoryButton.setMessage(Text.literal((this.category == Category.HUNGER_TWEAKS ? "> " : "") + Category.HUNGER_TWEAKS.label));

        SecondaryChatSettings.Data secondary = SecondaryChatSettings.get();
        ensureRegexSelectionInBounds();

        this.macroOverlayToggleButton.setMessage(Text.literal("Macro Keybind HUD: " + (ModConfig.showMacroKeybindOverlay ? "ON" : "OFF")));
        this.nbtHudToggleButton.setMessage(Text.literal("NBT Inspector HUD: " + (NBTInfoHudModule.INSTANCE.isEnabled() ? "ON" : "OFF")));

        this.secondaryEnabledToggleButton.setMessage(Text.literal("Secondary Chat: " + (SecondaryChatModule.INSTANCE.isEnabled() ? "ON" : "OFF")));
        this.secondaryOverlayToggleButton.setMessage(Text.literal("Secondary Overlay: " + (secondary.showOverlay ? "ON" : "OFF")));
        this.secondaryInterceptModeButton.setMessage(Text.literal("Intercept Mode: " + (secondary.interceptMode == null ? "COPY" : secondary.interceptMode.name())));

        this.pickupEnabledButton.setMessage(Text.literal("Pickup Notifier: " + (ItemPickupNotifierModule.INSTANCE.isEnabled() ? "ON" : "OFF")));
        this.pickupDurationButton.setMessage(Text.literal("Pickup Duration: " + PickupFeedSettings.get().durationMs + "ms"));
        this.pickupLinesButton.setMessage(Text.literal("Pickup Max Lines: " + PickupFeedSettings.get().maxLines));
        this.pickupIconScaleButton.setMessage(Text.literal("Pickup Icon Scale: " + String.format(Locale.ROOT, "%.2f", PickupFeedSettings.get().iconScale)));
        this.pickupDirectionButton.setMessage(Text.literal("Pickup Direction: " + PickupFeedSettings.get().direction.name()));

        this.freecamToggleButton.setMessage(Text.literal("Freecam: " + (FreecamModule.INSTANCE.isEnabled() ? "ON" : "OFF")));
        this.fullbrightToggleButton.setMessage(Text.literal("Fullbright: " + (FullbrightModule.INSTANCE.isEnabled() ? "ON" : "OFF")));
        this.heldLightToggleButton.setMessage(Text.literal("Held Light: " + (HeldLightModule.INSTANCE.isEnabled() ? "ON" : "OFF")));
        this.inventoryMoveToggleButton.setMessage(Text.literal("Inventory Move: " + (InventoryMoveModule.INSTANCE.isEnabled() ? "ON" : "OFF")));
        this.instantBreakToggleButton.setMessage(Text.literal("Instant Break: " + (InstantBreakModule.INSTANCE.isEnabled() ? "ON" : "OFF")));
        this.fastBlockPlacementToggleButton.setMessage(Text.literal("Fast Place: " + (FastBlockPlacementModule.INSTANCE.isEnabled() ? "ON" : "OFF")));
        this.betterCommandBlockUiToggleButton.setMessage(Text.literal("Better Command Block UI: " + (BetterCommandBlockUiModule.INSTANCE.isEnabled() ? "ON" : "OFF")));
        this.waypointsToggleButton.setMessage(Text.literal("Waypoints: " + (WaypointModule.INSTANCE.isEnabled() ? "ON" : "OFF")));
        this.nbtTooltipToggleButton.setMessage(Text.literal("NBT Tooltip: " + (NBTTooltipModule.INSTANCE.isEnabled() ? "ON" : "OFF")));
        this.shulkerTooltipToggleButton.setMessage(Text.literal("Shulker Preview Tooltip: " + (ShulkerTooltipModule.INSTANCE.isEnabled() ? "ON" : "OFF")));

        this.biomeBorderToggleButton.setMessage(Text.literal("Biome Border Overlay: " + (BiomeBorderOverlayModule.INSTANCE.isEnabled() ? "ON" : "OFF")));
        this.chunkBorderToggleButton.setMessage(Text.literal("Chunk Border Overlay: " + (ChunkBorderOverlayModule.INSTANCE.isEnabled() ? "ON" : "OFF")));
        this.slimeChunksToggleButton.setMessage(Text.literal("Slime Chunk Overlay: " + (SlimeChunkOverlayModule.INSTANCE.isEnabled() ? "ON" : "OFF")));
        this.structureBoundsToggleButton.setMessage(Text.literal("Structure Bounding Boxes: " + (StructureBoundingBoxOverlay.INSTANCE.isEnabled() ? "ON" : "OFF")));
        this.commandBlockOverlayToggleButton.setMessage(Text.literal("Command Block Overlay: " + (CommandBlockOverlayModule.INSTANCE.isEnabled() ? "ON" : "OFF")));
        this.lightOverlayToggleButton.setMessage(Text.literal("Light Level Overlay: " + (LightLevelOverlayModule.INSTANCE.isEnabled() ? "ON" : "OFF")));

        this.collisionMeshToggleButton.setMessage(Text.literal("Show Collision Mesh: " + (ModConfig.blockAttributesShowCollisionMesh ? "ON" : "OFF")));
        this.lightBlocksToggleButton.setMessage(Text.literal("Show Light Blocks: " + (ModConfig.blockAttributesShowLightBlocks ? "ON" : "OFF")));
        this.preventInteractionsToggleButton.setMessage(Text.literal("Prevent Interactions: " + (ModConfig.blockAttributesPreventInteractions ? "ON" : "OFF")));
        this.solidFluidHitboxesToggleButton.setMessage(Text.literal("Solid Fluid Hitboxes: " + (ModConfig.blockAttributesSolidFluidHitboxes ? "ON" : "OFF")));
        this.barrierBlocksToggleButton.setMessage(Text.literal("Show Barrier Blocks: " + (ModConfig.blockAttributesShowBarrierBlocks ? "ON" : "OFF")));

        this.tweaksModuleToggleButton.setMessage(Text.literal("Tweaks Module: " + (TweaksModule.INSTANCE.isEnabled() ? "ON" : "OFF")));
        this.hideOwnEffectsToggleButton.setMessage(Text.literal("Hide Own Effect Particles: " + (ModConfig.tweaksHideOwnEffectParticles ? "ON" : "OFF")));
        this.hideOffhandItemToggleButton.setMessage(Text.literal("Hide Offhand Item: " + (ModConfig.tweaksHideOffhandItem ? "ON" : "OFF")));
        this.disableBlockBreakParticlesToggleButton.setMessage(Text.literal("Disable Block Breaking Particles: " + (ModConfig.tweaksDisableBlockBreakingParticles ? "ON" : "OFF")));
        this.disableEntityRenderingToggleButton.setMessage(Text.literal("Disable Entity Rendering: " + (ModConfig.tweaksDisableEntityRendering ? "ON" : "OFF")));
        this.disableNetherFogToggleButton.setMessage(Text.literal("Disable Nether Fog: " + (ModConfig.tweaksDisableNetherFog ? "ON" : "OFF")));
        this.disableRenderDistanceFogToggleButton.setMessage(Text.literal("Disable Render-Distance Fog: " + (ModConfig.tweaksDisableRenderDistanceFog ? "ON" : "OFF")));
        this.disableRainEffectsToggleButton.setMessage(Text.literal("Disable Rain Effects: " + (ModConfig.tweaksDisableRainEffects ? "ON" : "OFF")));
        this.disableSoundsToggleButton.setMessage(Text.literal("Disable Sounds: " + (ModConfig.tweaksDisableSounds ? "ON" : "OFF")));
        this.disableWallUnsprintToggleButton.setMessage(Text.literal("Disable Wall Unsprint: " + (ModConfig.tweaksDisableWallUnsprint ? "ON" : "OFF")));
        this.angelBlockToggleButton.setMessage(Text.literal("Angel Block: " + (ModConfig.tweaksAngelBlock ? "ON" : "OFF")));
        this.permanentSneakToggleButton.setMessage(Text.literal("Permanent Sneak: " + (ModConfig.tweaksPermanentSneak ? "ON" : "OFF")));
        this.permanentSprintToggleButton.setMessage(Text.literal("Permanent Sprint: " + (ModConfig.tweaksPermanentSprint ? "ON" : "OFF")));
        this.disableHurtCameraToggleButton.setMessage(Text.literal("Disable Hurt Camera: " + (ModConfig.tweaksDisableHurtCamera ? "ON" : "OFF")));
        this.disableViewBobbingToggleButton.setMessage(Text.literal("Disable View Bobbing: " + (ModConfig.tweaksDisableViewBobbing ? "ON" : "OFF")));
        this.reachToggleButton.setMessage(Text.literal("Reach: " + (ReachModule.INSTANCE.isEnabled() ? "ON" : "OFF")));
        this.reachSafeClampToggleButton.setMessage(Text.literal("Reach Safe Multiplayer Clamp: " + (ModConfig.reachSafeMultiplayerClamp ? "ON" : "OFF")));
        this.reachBlockDistanceButton.setMessage(Text.literal("Reach Block Distance: " + String.format(Locale.ROOT, "%.2f", ModConfig.reachBlockDistance)));
        this.reachEntityDistanceButton.setMessage(Text.literal("Reach Entity Distance: " + String.format(Locale.ROOT, "%.2f", ModConfig.reachEntityDistance)));
        this.reachMpBlockExtraButton.setMessage(Text.literal("Reach MP Block Extra: " + String.format(Locale.ROOT, "%.2f", ModConfig.reachMultiplayerBlockExtra)));
        this.reachMpEntityExtraButton.setMessage(Text.literal("Reach MP Entity Extra: " + String.format(Locale.ROOT, "%.2f", ModConfig.reachMultiplayerEntityExtra)));
        this.mouseTweaksModuleToggleButton.setMessage(Text.literal("Mouse Tweaks Module: " + (MouseTweaksModule.INSTANCE.isEnabled() ? "ON" : "OFF")));
        this.mouseTweaksRmbToggleButton.setMessage(Text.literal("RMB Tweak: " + (ModConfig.mouseTweaksRmbTweak ? "ON" : "OFF")));
        this.mouseTweaksLmbWithItemToggleButton.setMessage(Text.literal("LMB Tweak With Item: " + (ModConfig.mouseTweaksLmbTweakWithItem ? "ON" : "OFF")));
        this.mouseTweaksLmbWithoutItemToggleButton.setMessage(Text.literal("LMB Tweak Without Item: " + (ModConfig.mouseTweaksLmbTweakWithoutItem ? "ON" : "OFF")));
        this.mouseTweaksWheelToggleButton.setMessage(Text.literal("Wheel Tweak: " + (ModConfig.mouseTweaksWheelTweak ? "ON" : "OFF")));
        this.mouseTweaksWheelSearchOrderButton.setMessage(Text.literal("Wheel Search Order: " + ModConfig.mouseTweaksWheelSearchOrder));
        this.mouseTweaksWheelScrollDirectionButton.setMessage(Text.literal("Wheel Scroll Direction: " + ModConfig.mouseTweaksWheelScrollDirection));
        this.mouseTweaksScrollScalingButton.setMessage(Text.literal("Scroll Item Scaling: " + ModConfig.mouseTweaksScrollItemScaling));
        this.hungerTweaksModuleToggleButton.setMessage(Text.literal("Hunger Tweaks Module: " + (HungerTweaksModule.INSTANCE.isEnabled() ? "ON" : "OFF")));
        this.hungerTooltipToggleButton.setMessage(Text.literal("Tooltip Food Values: " + (ModConfig.hungerTweaksShowFoodValuesInTooltip ? "ON" : "OFF")));
        this.hungerTooltipAlwaysToggleButton.setMessage(Text.literal("Tooltip Always Visible: " + (ModConfig.hungerTweaksShowFoodValuesInTooltipAlways ? "ON" : "OFF")));
        this.hungerSaturationOverlayToggleButton.setMessage(Text.literal("Saturation Overlay: " + (ModConfig.hungerTweaksShowSaturationHudOverlay ? "ON" : "OFF")));
        this.hungerFoodValuesOverlayToggleButton.setMessage(Text.literal("Held Food Overlay: " + (ModConfig.hungerTweaksShowFoodValuesHudOverlay ? "ON" : "OFF")));
        this.hungerOffhandOverlayToggleButton.setMessage(Text.literal("Offhand Overlay: " + (ModConfig.hungerTweaksShowFoodValuesHudOverlayWhenOffhand ? "ON" : "OFF")));
        this.hungerExhaustionUnderlayToggleButton.setMessage(Text.literal("Exhaustion Underlay: " + (ModConfig.hungerTweaksShowFoodExhaustionHudUnderlay ? "ON" : "OFF")));
        this.hungerHealthOverlayToggleButton.setMessage(Text.literal("Estimated Health Overlay: " + (ModConfig.hungerTweaksShowFoodHealthHudOverlay ? "ON" : "OFF")));
        this.hungerDebugInfoToggleButton.setMessage(Text.literal("Debug HUD Food Info: " + (ModConfig.hungerTweaksShowFoodDebugInfo ? "ON" : "OFF")));
        this.hungerVanillaAnimationToggleButton.setMessage(Text.literal("Match Vanilla Animation: " + (ModConfig.hungerTweaksShowVanillaAnimationsOverlay ? "ON" : "OFF")));
        this.hungerMaxFlashAlphaButton.setMessage(Text.literal("Max Flash Alpha: " + HungerTweaksModule.formatFlashAlpha(ModConfig.hungerTweaksMaxHudOverlayFlashAlpha)));

        if (!this.secondaryOutgoingRegexField.isFocused()) {
            this.secondaryOutgoingRegexField.setText(secondary.outgoingRegex == null ? "" : secondary.outgoingRegex);
        }

        boolean hud = this.category == Category.HUD;
        setVisible(this.macroOverlayToggleButton, hud);
        setVisible(this.nbtHudToggleButton, hud);

        boolean secondaryCategory = this.category == Category.SECONDARY_CHAT;
        setVisible(this.secondaryEnabledToggleButton, secondaryCategory);
        setVisible(this.secondaryOverlayToggleButton, secondaryCategory);
        setVisible(this.secondaryInterceptModeButton, secondaryCategory);
        setVisible(this.secondaryRegexInputField, secondaryCategory);
        setVisible(this.secondaryRegexAddButton, secondaryCategory);
        setVisible(this.secondaryRegexApplyButton, secondaryCategory);
        setVisible(this.secondaryRegexRemoveButton, secondaryCategory);
        setVisible(this.secondaryRegexClearButton, secondaryCategory);
        setVisible(this.secondaryOutgoingRegexField, secondaryCategory);
        setVisible(this.secondaryOutgoingApplyButton, secondaryCategory);

        boolean pickupCategory = this.category == Category.PICKUP_FEED;
        setVisible(this.pickupEnabledButton, pickupCategory);
        setVisible(this.pickupDurationButton, pickupCategory);
        setVisible(this.pickupLinesButton, pickupCategory);
        setVisible(this.pickupIconScaleButton, pickupCategory);
        setVisible(this.pickupDirectionButton, pickupCategory);

        boolean overlays = this.category == Category.OVERLAYS;
        setVisible(this.biomeBorderToggleButton, overlays);
        setVisible(this.chunkBorderToggleButton, overlays);
        setVisible(this.slimeChunksToggleButton, overlays);
        setVisible(this.structureBoundsToggleButton, overlays);
        setVisible(this.commandBlockOverlayToggleButton, overlays);
        setVisible(this.lightOverlayToggleButton, overlays);

        boolean modules = this.category == Category.MODULES;
        setVisible(this.freecamToggleButton, modules);
        setVisible(this.fullbrightToggleButton, modules);
        setVisible(this.heldLightToggleButton, modules);
        setVisible(this.inventoryMoveToggleButton, modules);
        setVisible(this.instantBreakToggleButton, modules);
        setVisible(this.fastBlockPlacementToggleButton, modules);
        setVisible(this.betterCommandBlockUiToggleButton, modules);
        setVisible(this.waypointsToggleButton, modules);
        setVisible(this.nbtTooltipToggleButton, modules);
        setVisible(this.shulkerTooltipToggleButton, modules);

        boolean blockAttributes = this.category == Category.BLOCK_ATTRIBUTES;
        setVisible(this.collisionMeshToggleButton, blockAttributes);
        setVisible(this.lightBlocksToggleButton, blockAttributes);
        setVisible(this.preventInteractionsToggleButton, blockAttributes);
        setVisible(this.solidFluidHitboxesToggleButton, blockAttributes);
        setVisible(this.barrierBlocksToggleButton, blockAttributes);

        boolean tweaks = this.category == Category.TWEAKS;
        setVisible(this.tweaksModuleToggleButton, tweaks);
        setVisible(this.hideOwnEffectsToggleButton, tweaks);
        setVisible(this.hideOffhandItemToggleButton, tweaks);
        setVisible(this.disableBlockBreakParticlesToggleButton, tweaks);
        setVisible(this.disableEntityRenderingToggleButton, tweaks);
        setVisible(this.disableNetherFogToggleButton, tweaks);
        setVisible(this.disableRenderDistanceFogToggleButton, tweaks);
        setVisible(this.disableRainEffectsToggleButton, tweaks);
        setVisible(this.disableSoundsToggleButton, tweaks);
        setVisible(this.disableWallUnsprintToggleButton, tweaks);
        setVisible(this.angelBlockToggleButton, tweaks);
        setVisible(this.permanentSneakToggleButton, tweaks);
        setVisible(this.permanentSprintToggleButton, tweaks);
        setVisible(this.disableHurtCameraToggleButton, tweaks);
        setVisible(this.disableViewBobbingToggleButton, tweaks);
        setVisible(this.reachToggleButton, tweaks);
        setVisible(this.reachSafeClampToggleButton, tweaks);
        setVisible(this.reachBlockDistanceButton, tweaks);
        setVisible(this.reachEntityDistanceButton, tweaks);
        setVisible(this.reachMpBlockExtraButton, tweaks);
        setVisible(this.reachMpEntityExtraButton, tweaks);

        boolean mouseTweaks = this.category == Category.MOUSE_TWEAKS;
        setVisible(this.mouseTweaksModuleToggleButton, mouseTweaks);
        setVisible(this.mouseTweaksRmbToggleButton, mouseTweaks);
        setVisible(this.mouseTweaksLmbWithItemToggleButton, mouseTweaks);
        setVisible(this.mouseTweaksLmbWithoutItemToggleButton, mouseTweaks);
        setVisible(this.mouseTweaksWheelToggleButton, mouseTweaks);
        setVisible(this.mouseTweaksWheelSearchOrderButton, mouseTweaks);
        setVisible(this.mouseTweaksWheelScrollDirectionButton, mouseTweaks);
        setVisible(this.mouseTweaksScrollScalingButton, mouseTweaks);

        boolean hungerTweaks = this.category == Category.HUNGER_TWEAKS;
        setVisible(this.hungerTweaksModuleToggleButton, hungerTweaks);
        setVisible(this.hungerTooltipToggleButton, hungerTweaks);
        setVisible(this.hungerTooltipAlwaysToggleButton, hungerTweaks);
        setVisible(this.hungerSaturationOverlayToggleButton, hungerTweaks);
        setVisible(this.hungerFoodValuesOverlayToggleButton, hungerTweaks);
        setVisible(this.hungerOffhandOverlayToggleButton, hungerTweaks);
        setVisible(this.hungerExhaustionUnderlayToggleButton, hungerTweaks);
        setVisible(this.hungerHealthOverlayToggleButton, hungerTweaks);
        setVisible(this.hungerDebugInfoToggleButton, hungerTweaks);
        setVisible(this.hungerVanillaAnimationToggleButton, hungerTweaks);
        setVisible(this.hungerMaxFlashAlphaButton, hungerTweaks);
    }

    void render(DrawContext context) {
        syncControls();
        int splitX = this.owner.width / 2;
        context.fill(splitX - 1, TOP_BAR_H + 4, splitX, this.owner.height - 8, 0x50FFFFFF);
        context.drawTextWithShadow(this.owner.workbenchTextRenderer(), "Categories", 12, TOP_BAR_H + 6, 0xFFFFFFFF);
        context.drawTextWithShadow(this.owner.workbenchTextRenderer(), "Configuration", splitX + 12, TOP_BAR_H + 6, 0xFFFFFFFF);

        if (this.category == Category.SECONDARY_CHAT) {
            renderSecondaryRegexList(context);
            context.drawTextWithShadow(this.owner.workbenchTextRenderer(),
                    "Tip: select a regex row, edit in field, then Apply Selected.", splitX + 12, rowY(11), 0xFFA8CFCF);
            return;
        }

        String description = switch (this.category) {
            case HUD -> "HUD toggles and overlays rendered in screen space.";
            case OVERLAYS -> "World overlays (chunks, structures, light, command blocks).";
            case MODULES -> "General gameplay modules and tooltip behaviors.";
            case PICKUP_FEED -> "Pick-up feed module toggle and behavior settings.";
            case BLOCK_ATTRIBUTES -> "Block interaction and hitbox behavior overrides.";
            case TWEAKS -> "Visual and gameplay tweaks, plus reach controls.";
            case MOUSE_TWEAKS -> "Inventory mouse drag and wheel-transfer tweaks.";
            case HUNGER_TWEAKS -> "Food tooltip, saturation, exhaustion, and healing prediction overlays.";
            default -> "";
        };
        context.drawTextWithShadow(this.owner.workbenchTextRenderer(), description, splitX + 12, rowY(8), 0xFFA8CFCF);
    }

    boolean handleMouseClick(double mouseX, double mouseY, int button) {
        if (this.category == Category.PICKUP_FEED && (button == 0 || button == 1)) {
            int direction = button == 0 ? 1 : -1;
            if (contains(mouseX, mouseY, this.pickupDurationButton)) {
                adjustPickupDuration(direction);
                syncControls();
                return true;
            }
            if (contains(mouseX, mouseY, this.pickupLinesButton)) {
                adjustPickupLines(direction);
                syncControls();
                return true;
            }
            if (contains(mouseX, mouseY, this.pickupIconScaleButton)) {
                adjustPickupIconScale(direction);
                syncControls();
                return true;
            }
            if (contains(mouseX, mouseY, this.pickupDirectionButton)) {
                cyclePickupDirection(direction > 0);
                syncControls();
                return true;
            }
            return false;
        }

        if (this.category == Category.TWEAKS && button == 1) {
            if (contains(mouseX, mouseY, this.reachBlockDistanceButton)) {
                adjustReachBlockDistance(-1);
                syncControls();
                return true;
            }
            if (contains(mouseX, mouseY, this.reachEntityDistanceButton)) {
                adjustReachEntityDistance(-1);
                syncControls();
                return true;
            }
            if (contains(mouseX, mouseY, this.reachMpBlockExtraButton)) {
                adjustReachMpBlockExtra(-1);
                syncControls();
                return true;
            }
            if (contains(mouseX, mouseY, this.reachMpEntityExtraButton)) {
                adjustReachMpEntityExtra(-1);
                syncControls();
                return true;
            }
            return false;
        }

        if (this.category == Category.HUNGER_TWEAKS && (button == 0 || button == 1)) {
            int direction = button == 0 ? 1 : -1;
            if (contains(mouseX, mouseY, this.hungerMaxFlashAlphaButton)) {
                adjustHungerMaxFlashAlpha(direction);
                syncControls();
                return true;
            }
            return false;
        }

        if (button != 0 || this.category != Category.SECONDARY_CHAT) {
            return false;
        }
        int[] rect = regexListRect();
        int x = rect[0];
        int y = rect[1];
        int w = rect[2];
        int h = rect[3];
        if (!contains(mouseX, mouseY, x, y, w, h)) {
            return false;
        }

        List<String> regexes = SecondaryChatSettings.get().regexList;
        int rowHeight = 12;
        int visibleRows = Math.max(1, h / rowHeight);
        int clicked = (int) ((mouseY - y) / rowHeight);
        int index = this.regexScroll + clicked;
        if (index >= 0 && index < regexes.size()) {
            this.selectedRegexIndex = index;
            this.secondaryRegexInputField.setText(regexes.get(index));
            this.secondaryRegexInputField.setCursorToEnd(false);
            return true;
        }
        return false;
    }

    boolean handleMouseScroll(double mouseX, double mouseY, double verticalAmount) {
        if (this.category != Category.SECONDARY_CHAT) {
            return false;
        }
        int[] rect = regexListRect();
        if (!contains(mouseX, mouseY, rect[0], rect[1], rect[2], rect[3])) {
            return false;
        }
        int delta = verticalAmount > 0 ? -1 : 1;
        int max = Math.max(0, SecondaryChatSettings.get().regexList.size() - Math.max(1, rect[3] / 12));
        this.regexScroll = Math.clamp(this.regexScroll + delta, 0, max);
        return true;
    }

    private void renderSecondaryRegexList(DrawContext context) {
        int[] rect = regexListRect();
        int x = rect[0];
        int y = rect[1];
        int w = rect[2];
        int h = rect[3];
        int rowHeight = 12;

        context.fill(x, y, x + w, y + h, 0xAA111111);
        context.fill(x, y, x + w, y + 1, 0x60FFFFFF);

        List<String> regexes = SecondaryChatSettings.get().regexList;
        int visibleRows = Math.max(1, h / rowHeight);
        int max = Math.max(0, regexes.size() - visibleRows);
        this.regexScroll = Math.clamp(this.regexScroll, 0, max);

        for (int i = 0; i < visibleRows; i++) {
            int idx = this.regexScroll + i;
            if (idx >= regexes.size()) {
                break;
            }
            int yy = y + i * rowHeight;
            if (idx == this.selectedRegexIndex) {
                context.fill(x + 1, yy, x + w - 1, yy + rowHeight, 0x604A7CC7);
            }
            String line = regexes.get(idx);
            String text = (idx + 1) + ". " + line;
            if (this.owner.workbenchTextRenderer().getWidth(text) > w - 6) {
                while (text.length() > 8 && this.owner.workbenchTextRenderer().getWidth(text + "...") > w - 6) {
                    text = text.substring(0, text.length() - 1);
                }
                text += "...";
            }
            context.drawTextWithShadow(this.owner.workbenchTextRenderer(), text, x + 3, yy + 2, 0xFFE0E0E0);
        }

        if (regexes.isEmpty()) {
            context.drawTextWithShadow(this.owner.workbenchTextRenderer(), "(none)", x + 3, y + 3, 0xFF909090);
        }
    }

    private int[] regexListRect() {
        int rightX = (this.owner.width / 2) + 12;
        int settingW = Math.max(180, this.owner.width - rightX - 12);
        return new int[]{rightX, rowY(4), settingW, 58};
    }

    private void ensureRegexSelectionInBounds() {
        List<String> regexes = SecondaryChatSettings.get().regexList;
        if (regexes == null || regexes.isEmpty()) {
            this.selectedRegexIndex = -1;
            return;
        }
        this.selectedRegexIndex = Math.clamp(this.selectedRegexIndex, 0, regexes.size() - 1);
    }

    private void setCategory(Category category) {
        this.category = category == null ? Category.HUD : category;
        syncControls();
    }

    private static String safeField(TextFieldWidget field) {
        if (field == null || field.getText() == null) {
            return "";
        }
        return field.getText().trim();
    }

    private int rowY(int index) {
        return CONTENT_START_Y + (index * ROW_GAP);
    }

    private ButtonWidget button(String label, ButtonWidget.PressAction action, int x, int y, int width, int height) {
        return ButtonWidget.builder(Text.literal(label), action)
                .dimensions(x, y, width, height)
                .build();
    }

    private void register(ClickableWidget... widgets) {
        for (ClickableWidget widget : widgets) {
            this.configWidgets.add(widget);
            this.owner.addDrawableChild(widget);
        }
    }

    private static void setVisible(ClickableWidget widget, boolean visible) {
        widget.visible = visible;
        widget.active = visible;
    }

    private static boolean contains(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static boolean contains(double mx, double my, ClickableWidget widget) {
        return widget != null
                && widget.visible
                && contains(mx, my, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight());
    }

    private void adjustPickupDuration(int direction) {
        PickupFeedSettings.updateAndSave(() -> {
            int step = this.shiftDown.getAsBoolean() ? 250 : 500;
            PickupFeedSettings.get().durationMs += direction * step;
        });
    }

    private void adjustPickupLines(int direction) {
        PickupFeedSettings.updateAndSave(() -> {
            int step = this.shiftDown.getAsBoolean() ? 1 : 2;
            PickupFeedSettings.get().maxLines += direction * step;
        });
    }

    private void adjustPickupIconScale(int direction) {
        PickupFeedSettings.updateAndSave(() -> {
            float step = this.shiftDown.getAsBoolean() ? 0.05f : 0.1f;
            PickupFeedSettings.get().iconScale += direction * step;
        });
    }

    private void cyclePickupDirection(boolean forward) {
        PickupFeedSettings.updateAndSave(() -> {
            PickupFeedSettings.Direction current = PickupFeedSettings.get().direction;
            if (forward) {
                PickupFeedSettings.get().direction = current == PickupFeedSettings.Direction.UP
                        ? PickupFeedSettings.Direction.DOWN
                        : PickupFeedSettings.Direction.UP;
            } else {
                PickupFeedSettings.get().direction = current == PickupFeedSettings.Direction.DOWN
                        ? PickupFeedSettings.Direction.UP
                        : PickupFeedSettings.Direction.DOWN;
            }
        });
    }

    private void adjustReachBlockDistance(int direction) {
        ModConfig.updateAndSave(() -> {
            double step = this.shiftDown.getAsBoolean() ? 0.5 : 0.25;
            ModConfig.reachBlockDistance = Math.clamp(ModConfig.reachBlockDistance + (direction * step), 1.0, 16.0);
        });
    }

    private void adjustReachEntityDistance(int direction) {
        ModConfig.updateAndSave(() -> {
            double step = this.shiftDown.getAsBoolean() ? 0.5 : 0.25;
            ModConfig.reachEntityDistance = Math.clamp(ModConfig.reachEntityDistance + (direction * step), 1.0, 16.0);
        });
    }

    private void adjustReachMpBlockExtra(int direction) {
        ModConfig.updateAndSave(() -> {
            double step = this.shiftDown.getAsBoolean() ? 0.5 : 0.25;
            ModConfig.reachMultiplayerBlockExtra = Math.clamp(ModConfig.reachMultiplayerBlockExtra + (direction * step), 0.0, 4.0);
        });
    }

    private void adjustReachMpEntityExtra(int direction) {
        ModConfig.updateAndSave(() -> {
            double step = this.shiftDown.getAsBoolean() ? 0.5 : 0.25;
            ModConfig.reachMultiplayerEntityExtra = Math.clamp(ModConfig.reachMultiplayerEntityExtra + (direction * step), 0.0, 4.0);
        });
    }

    private void adjustHungerMaxFlashAlpha(int direction) {
        ModConfig.updateAndSave(() -> {
            float step = this.shiftDown.getAsBoolean() ? 0.01f : 0.05f;
            ModConfig.hungerTweaksMaxHudOverlayFlashAlpha = HungerTweaksModule.clampFlashAlpha(
                    ModConfig.hungerTweaksMaxHudOverlayFlashAlpha + (direction * step)
            );
        });
    }

    private static MouseTweaksWheelSearchOrder nextWheelSearchOrder(MouseTweaksWheelSearchOrder current) {
        return current == MouseTweaksWheelSearchOrder.FIRST_TO_LAST
                ? MouseTweaksWheelSearchOrder.LAST_TO_FIRST
                : MouseTweaksWheelSearchOrder.FIRST_TO_LAST;
    }

    private static MouseTweaksWheelScrollDirection nextWheelScrollDirection(MouseTweaksWheelScrollDirection current) {
        MouseTweaksWheelScrollDirection[] values = MouseTweaksWheelScrollDirection.values();
        return values[(current.ordinal() + 1) % values.length];
    }

    private static MouseTweaksScrollItemScaling nextScrollScaling(MouseTweaksScrollItemScaling current) {
        return current == MouseTweaksScrollItemScaling.PROPORTIONAL
                ? MouseTweaksScrollItemScaling.ALWAYS_ONE
                : MouseTweaksScrollItemScaling.PROPORTIONAL;
    }
}
