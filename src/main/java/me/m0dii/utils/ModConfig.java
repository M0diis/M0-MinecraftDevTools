package me.m0dii.utils;


import eu.midnightdust.lib.config.MidnightConfig;
import me.m0dii.M0DevToolsClient;
import me.m0dii.modules.bridging.BridgingAdjacency;
import me.m0dii.modules.bridging.BridgingAxisMode;
import me.m0dii.modules.bridging.BridgingAxisModeOverride;
import me.m0dii.modules.bridging.BridgingPerspectiveLock;
import me.m0dii.modules.mousetweaks.MouseTweaksScrollItemScaling;
import me.m0dii.modules.mousetweaks.MouseTweaksWheelScrollDirection;
import me.m0dii.modules.mousetweaks.MouseTweaksWheelSearchOrder;

@SuppressWarnings({"CanBeFinal", "java:S1444"})
public class ModConfig extends MidnightConfig {

    public static void save() {
        MidnightConfig.write(M0DevToolsClient.MOD_ID);
    }

    public static void updateAndSave(Runnable change) {
        if (change == null) {
            return;
        }
        change.run();
        save();
    }

    public static final String CATEGORY_GENERAL = "General";

    @MidnightConfig.Entry(name = "Fullbright Gamma", category = CATEGORY_GENERAL, isSlider = true, min = 1, max = 100)
    public static double fullbrightGamma = 100;

    @MidnightConfig.Entry(name = "Command History Limit", category = CATEGORY_GENERAL, isSlider = true, min = 10, max = 100)
    public static int commandHistoryLimit = 500;

    @MidnightConfig.Entry(name = "Message Box History Limit", category = CATEGORY_GENERAL, min = 10, max = 50000)
    public static int messageBoxHistoryLimit = 500;

    @MidnightConfig.Entry(name = "Message History Limit", category = CATEGORY_GENERAL, min = 10, max = 1000)
    public static int messageHistoryLimit = 500;

    public static final String CATEGORY_PERFORMANCE = "Performance";

    @MidnightConfig.Entry(name = "Dynamic FPS Enabled", category = CATEGORY_PERFORMANCE)
    public static boolean dynamicFpsEnabled = true;

    @MidnightConfig.Entry(name = "Dynamic FPS Unfocused Cap", category = CATEGORY_PERFORMANCE, isSlider = true, min = 5, max = 260)
    public static int dynamicFpsUnfocusedFps = 30;

    public static final String CATEGORY_TWEAKS = "Tweaks";

    @MidnightConfig.Entry(name = "Tweaks Module Enabled", category = CATEGORY_TWEAKS)
    public static boolean tweaksModuleEnabled = true;

    @MidnightConfig.Entry(name = "Hide Own Effect Particles", category = CATEGORY_TWEAKS)
    public static boolean tweaksHideOwnEffectParticles = false;

    @MidnightConfig.Entry(name = "Hide Offhand Item", category = CATEGORY_TWEAKS)
    public static boolean tweaksHideOffhandItem = false;

    @MidnightConfig.Entry(name = "Disable Block Breaking Particles", category = CATEGORY_TWEAKS)
    public static boolean tweaksDisableBlockBreakingParticles = false;

    @MidnightConfig.Entry(name = "Disable Entity Rendering", category = CATEGORY_TWEAKS)
    public static boolean tweaksDisableEntityRendering = false;

    @MidnightConfig.Entry(name = "Disable Nether Fog", category = CATEGORY_TWEAKS)
    public static boolean tweaksDisableNetherFog = false;

    @MidnightConfig.Entry(name = "Disable Rain Effects", category = CATEGORY_TWEAKS)
    public static boolean tweaksDisableRainEffects = false;

    @MidnightConfig.Entry(name = "Disable Sounds", category = CATEGORY_TWEAKS)
    public static boolean tweaksDisableSounds = false;

    @MidnightConfig.Entry(name = "Disable Wall Unsprint", category = CATEGORY_TWEAKS)
    public static boolean tweaksDisableWallUnsprint = false;

    @MidnightConfig.Entry(name = "Fast Block Placement", category = CATEGORY_TWEAKS)
    public static boolean tweaksFastBlockPlacement = false;

    @MidnightConfig.Entry(name = "Fast Block Placement Enabled", category = CATEGORY_TWEAKS)
    public static boolean fastBlockPlacementEnabled = false;

    @MidnightConfig.Entry(name = "Angel Block", category = CATEGORY_TWEAKS)
    public static boolean tweaksAngelBlock = false;

    @MidnightConfig.Entry(name = "Permanent Sneak", category = CATEGORY_TWEAKS)
    public static boolean tweaksPermanentSneak = false;

    @MidnightConfig.Entry(name = "Permanent Sprint", category = CATEGORY_TWEAKS)
    public static boolean tweaksPermanentSprint = false;

    @MidnightConfig.Entry(name = "Disable Hurt Camera", category = CATEGORY_TWEAKS)
    public static boolean tweaksDisableHurtCamera = false;

    @MidnightConfig.Entry(name = "Disable View Bobbing", category = CATEGORY_TWEAKS)
    public static boolean tweaksDisableViewBobbing = false;

    @MidnightConfig.Entry(name = "Disable Render Distance Fog", category = CATEGORY_TWEAKS)
    public static boolean tweaksDisableRenderDistanceFog = false;

    public static final String CATEGORY_BRIDGING_TWEAKS = "Bridging Tweaks";

    @MidnightConfig.Entry(name = "Bridging Tweaks Module Enabled", category = CATEGORY_BRIDGING_TWEAKS)
    public static boolean bridgingTweaksEnabled = true;

    @MidnightConfig.Entry(name = "Minimum Bridge Distance (%)", category = CATEGORY_BRIDGING_TWEAKS, isSlider = true, min = 0, max = 100)
    public static float bridgingMinBridgeDistance = 20.0f;

    @MidnightConfig.Entry(name = "Only Bridge When Crouched", category = CATEGORY_BRIDGING_TWEAKS)
    public static boolean bridgingOnlyWhenCrouched = false;

    @MidnightConfig.Entry(name = "Supported Bridge Axes", category = CATEGORY_BRIDGING_TWEAKS)
    public static BridgingAxisMode bridgingSupportedAxes = BridgingAxisMode.BOTH;

    @MidnightConfig.Entry(name = "Supported Bridge Axes When Crouched", category = CATEGORY_BRIDGING_TWEAKS)
    public static BridgingAxisModeOverride bridgingSupportedAxesWhenCrouched = BridgingAxisModeOverride.FALLBACK;

    @MidnightConfig.Entry(name = "Delay Post Bridging", category = CATEGORY_BRIDGING_TWEAKS, isSlider = true, min = 0, max = 20)
    public static int bridgingDelayPostBridging = 4;

    @MidnightConfig.Entry(name = "Show Crosshair Indicator", category = CATEGORY_BRIDGING_TWEAKS)
    public static boolean bridgingShowCrosshair = true;

    @MidnightConfig.Entry(name = "Show Bridging Outline", category = CATEGORY_BRIDGING_TWEAKS)
    public static boolean bridgingShowOutline = false;

    @MidnightConfig.Entry(name = "Show Outline When Not Bridging", category = CATEGORY_BRIDGING_TWEAKS)
    public static boolean bridgingShowOutlineWhenNotBridging = false;

    @MidnightConfig.Entry(name = "Non-Bridge Outline Respects Crouch Rules", category = CATEGORY_BRIDGING_TWEAKS)
    public static boolean bridgingNonBridgeRespectsCrouchRules = true;

    @MidnightConfig.Entry(name = "Outline Color (ARGB)", category = CATEGORY_BRIDGING_TWEAKS)
    public static int bridgingOutlineColor = 0x66000000;

    @MidnightConfig.Entry(name = "Skip Torch Bridging", category = CATEGORY_BRIDGING_TWEAKS)
    public static boolean bridgingSkipTorchBridging = true;

    @MidnightConfig.Entry(name = "Enable Slab Assist", category = CATEGORY_BRIDGING_TWEAKS)
    public static boolean bridgingEnableSlabAssist = true;

    @MidnightConfig.Entry(name = "Enable Non-Solid Replace", category = CATEGORY_BRIDGING_TWEAKS)
    public static boolean bridgingEnableNonSolidReplace = true;

    @MidnightConfig.Entry(name = "Bridging Snap Strength", category = CATEGORY_BRIDGING_TWEAKS, isSlider = true, min = 0, max = 1)
    public static float bridgingSnapStrength = 1.0f;

    @MidnightConfig.Entry(name = "Bridging Adjacency", category = CATEGORY_BRIDGING_TWEAKS)
    public static BridgingAdjacency bridgingAdjacency = BridgingAdjacency.CORNERS;

    @MidnightConfig.Entry(name = "Perspective Lock", category = CATEGORY_BRIDGING_TWEAKS)
    public static BridgingPerspectiveLock bridgingPerspectiveLock = BridgingPerspectiveLock.LET_BRIDGING_DECIDE;

    @MidnightConfig.Entry(name = "Show Debug Highlight", category = CATEGORY_BRIDGING_TWEAKS)
    public static boolean bridgingShowDebugHighlight = true;

    @MidnightConfig.Entry(name = "Show Debug Non-Bridging Highlight", category = CATEGORY_BRIDGING_TWEAKS)
    public static boolean bridgingShowDebugNonBridgingHighlight = false;

    @MidnightConfig.Entry(name = "Show Debug Trace", category = CATEGORY_BRIDGING_TWEAKS)
    public static boolean bridgingShowDebugTrace = false;

    public static final String CATEGORY_MOUSE_TWEAKS = "Mouse Tweaks";

    @MidnightConfig.Entry(name = "Mouse Tweaks Module Enabled", category = CATEGORY_MOUSE_TWEAKS)
    public static boolean mouseTweaksModuleEnabled = true;

    @MidnightConfig.Entry(name = "RMB Tweak", category = CATEGORY_MOUSE_TWEAKS)
    public static boolean mouseTweaksRmbTweak = true;

    @MidnightConfig.Entry(name = "LMB Tweak With Item", category = CATEGORY_MOUSE_TWEAKS)
    public static boolean mouseTweaksLmbTweakWithItem = true;

    @MidnightConfig.Entry(name = "LMB Tweak Without Item", category = CATEGORY_MOUSE_TWEAKS)
    public static boolean mouseTweaksLmbTweakWithoutItem = true;

    @MidnightConfig.Entry(name = "Wheel Tweak", category = CATEGORY_MOUSE_TWEAKS)
    public static boolean mouseTweaksWheelTweak = true;

    @MidnightConfig.Entry(name = "Wheel Search Order", category = CATEGORY_MOUSE_TWEAKS)
    public static MouseTweaksWheelSearchOrder mouseTweaksWheelSearchOrder = MouseTweaksWheelSearchOrder.LAST_TO_FIRST;

    @MidnightConfig.Entry(name = "Wheel Scroll Direction", category = CATEGORY_MOUSE_TWEAKS)
    public static MouseTweaksWheelScrollDirection mouseTweaksWheelScrollDirection = MouseTweaksWheelScrollDirection.NORMAL;

    @MidnightConfig.Entry(name = "Scroll Item Scaling", category = CATEGORY_MOUSE_TWEAKS)
    public static MouseTweaksScrollItemScaling mouseTweaksScrollItemScaling = MouseTweaksScrollItemScaling.PROPORTIONAL;

    public static final String CATEGORY_HUNGER_TWEAKS = "Hunger Tweaks";

    @MidnightConfig.Entry(name = "Hunger Tweaks Module Enabled", category = CATEGORY_HUNGER_TWEAKS)
    public static boolean hungerTweaksModuleEnabled = true;

    @MidnightConfig.Entry(name = "Show Food Values In Tooltip", category = CATEGORY_HUNGER_TWEAKS)
    public static boolean hungerTweaksShowFoodValuesInTooltip = true;

    @MidnightConfig.Entry(name = "Always Show Food Values In Tooltip", category = CATEGORY_HUNGER_TWEAKS)
    public static boolean hungerTweaksShowFoodValuesInTooltipAlways = true;

    @MidnightConfig.Entry(name = "Show Saturation HUD Overlay", category = CATEGORY_HUNGER_TWEAKS)
    public static boolean hungerTweaksShowSaturationHudOverlay = true;

    @MidnightConfig.Entry(name = "Show Food Values HUD Overlay", category = CATEGORY_HUNGER_TWEAKS)
    public static boolean hungerTweaksShowFoodValuesHudOverlay = true;

    @MidnightConfig.Entry(name = "Show Food Values HUD Overlay When Offhand", category = CATEGORY_HUNGER_TWEAKS)
    public static boolean hungerTweaksShowFoodValuesHudOverlayWhenOffhand = true;

    @MidnightConfig.Entry(name = "Show Food Exhaustion HUD Underlay", category = CATEGORY_HUNGER_TWEAKS)
    public static boolean hungerTweaksShowFoodExhaustionHudUnderlay = true;

    @MidnightConfig.Entry(name = "Show Food Health HUD Overlay", category = CATEGORY_HUNGER_TWEAKS)
    public static boolean hungerTweaksShowFoodHealthHudOverlay = true;

    @MidnightConfig.Entry(name = "Show Food Debug Info", category = CATEGORY_HUNGER_TWEAKS)
    public static boolean hungerTweaksShowFoodDebugInfo = true;

    @MidnightConfig.Entry(name = "Show Vanilla Animations Overlay", category = CATEGORY_HUNGER_TWEAKS)
    public static boolean hungerTweaksShowVanillaAnimationsOverlay = true;

    @MidnightConfig.Entry(name = "Max HUD Overlay Flash Alpha", category = CATEGORY_HUNGER_TWEAKS, isSlider = true, min = 0, max = 1)
    public static float hungerTweaksMaxHudOverlayFlashAlpha = 0.65f;

    public static final String CATEGORY_REACH = "Reach";

    @MidnightConfig.Entry(name = "Reach Module Enabled", category = CATEGORY_REACH)
    public static boolean reachModuleEnabled = false;

    @MidnightConfig.Entry(name = "Block Reach Distance", category = CATEGORY_REACH, isSlider = true, min = 1, max = 16)
    public static double reachBlockDistance = 4.5;

    @MidnightConfig.Entry(name = "Entity Reach Distance", category = CATEGORY_REACH, isSlider = true, min = 1, max = 16)
    public static double reachEntityDistance = 3.0;

    @MidnightConfig.Entry(name = "Safe Multiplayer Clamp", category = CATEGORY_REACH)
    public static boolean reachSafeMultiplayerClamp = true;

    @MidnightConfig.Entry(name = "Multiplayer Block Reach Extra", category = CATEGORY_REACH, isSlider = true, min = 0, max = 4)
    public static double reachMultiplayerBlockExtra = 1.0;

    @MidnightConfig.Entry(name = "Multiplayer Entity Reach Extra", category = CATEGORY_REACH, isSlider = true, min = 0, max = 4)
    public static double reachMultiplayerEntityExtra = 1.0;

    public static final String CATEGORY_OVERLAY = "Overlay";

    @MidnightConfig.Entry(name = "Entity Radar Radius", category = CATEGORY_OVERLAY, isSlider = true, min = 16, max = 256)
    public static int entityRadarRadius = 64;

    // --- Inspector-specific HUD overlay settings ---
    public static final String CATEGORY_HUD_OVERLAY = "Hud";

    public enum OverlayAnchor {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    // --- Macro overlay-specific settings (for HUD overlays like keybinds/pending macros) ---
    @MidnightConfig.Entry(name = "Macro Overlay Text Scale", category = CATEGORY_HUD_OVERLAY)
    public static double macroOverlayTextScale = 0.85;

    @MidnightConfig.Entry(name = "Macro Overlay Line Height", category = CATEGORY_HUD_OVERLAY)
    public static int macroOverlayLineHeight = 9;

    @MidnightConfig.Entry(name = "Macro Overlay Padding", category = CATEGORY_HUD_OVERLAY)
    public static int macroOverlayPadding = 4;

    @MidnightConfig.Entry(name = "Macro Overlay Margin X", category = CATEGORY_HUD_OVERLAY)
    public static int macroOverlayMarginX = 6;

    @MidnightConfig.Entry(name = "Macro Overlay Margin Y", category = CATEGORY_HUD_OVERLAY)
    public static int macroOverlayMarginY = 6;

    @MidnightConfig.Entry(name = "Show Macro Keybind Overlay", category = CATEGORY_HUD_OVERLAY)
    public static boolean showMacroKeybindOverlay = true;

    public static final String CATEGORY_BLOCK_ATTRIBUTES = "Block Attributes";

    @MidnightConfig.Entry(name = "Show Collision Mesh", category = CATEGORY_BLOCK_ATTRIBUTES)
    public static boolean blockAttributesShowCollisionMesh = false;

    @MidnightConfig.Entry(name = "Show Light Blocks", category = CATEGORY_BLOCK_ATTRIBUTES)
    public static boolean blockAttributesShowLightBlocks = false;

    @MidnightConfig.Entry(name = "Prevent Interactions", category = CATEGORY_BLOCK_ATTRIBUTES)
    public static boolean blockAttributesPreventInteractions = false;

    @MidnightConfig.Entry(name = "Solid Fluid Hitboxes", category = CATEGORY_BLOCK_ATTRIBUTES)
    public static boolean blockAttributesSolidFluidHitboxes = false;

    @MidnightConfig.Entry(name = "Show Barrier Blocks", category = CATEGORY_BLOCK_ATTRIBUTES)
    public static boolean blockAttributesShowBarrierBlocks = false;

    @MidnightConfig.Entry(name = "Pending Macros Anchor", category = CATEGORY_HUD_OVERLAY)
    public static OverlayAnchor pendingMacrosAnchor = OverlayAnchor.TOP_LEFT;

    // --- ClickGUI specific settings ---
    @MidnightConfig.Entry(name = "ClickGUI Text Scale", category = CATEGORY_HUD_OVERLAY)
    public static double clickGuiTextScale = 0.85;

}
