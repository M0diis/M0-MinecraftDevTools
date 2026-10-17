package me.m0dii.modules.overlays;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import me.m0dii.modules.PersistentSettings;
import me.m0dii.utils.KeybindCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LightLevelOverlayModule extends BlockTextOverlayModule implements PersistentSettings {

    private int XZ_RADIUS = 16;
    private int Y_RADIUS = 4;
    private boolean spawnMode = false;

    private static final List<Block> excludedBlocks = List.of(
            Blocks.SHORT_GRASS,
            Blocks.TALL_GRASS,
            Blocks.FERN,
            Blocks.LARGE_FERN,
            Blocks.DEAD_BUSH,
            Blocks.SEAGRASS,
            Blocks.KELP,
            Blocks.SUGAR_CANE
    );

    public static final LightLevelOverlayModule INSTANCE = new LightLevelOverlayModule();

    private LightLevelOverlayModule() {
        super("light_overlay", "Light Overlay", false);
    }

    @Override
    public void register() {
        super.register();

        registerPressedKeybind(KeybindCatalog.LIGHT_OVERLAY_TOGGLE.translationKey(),
                InputConstants.Type.KEYBOARD,
                KeybindCatalog.LIGHT_OVERLAY_TOGGLE.defaultKey(),
                client -> toggleEnabled());
    }

    @Nullable
    @Override
    protected TextRenderInfo shouldRender(BlockPos pos, Vec3 cameraPos) {
        BlockState state = getClient().level.getBlockState(pos);
        if (state.isAir() || state.getLightEmission() > 0 || excludedBlocks.contains(state.getBlock())) {
            return null;
        }

        BlockPos abovePos = pos.above();
        BlockState above = getClient().level.getBlockState(abovePos);
        boolean topExposed = above.isAir() || above.getCollisionShape(getClient().level, abovePos).isEmpty();
        if (!topExposed) {
            return null;
        }

        if (spawnMode) {
            return spawnSpot(pos, state, abovePos, above);
        }

        int light = getClient().level.getRawBrightness(abovePos, 0);
        if (light <= 0) {
            return null;
        }

        int color = (light < 8) ? 0xFF0000 : 0x00FF00;
        return new TextRenderInfo(Integer.toString(light), color);
    }

    /** Marks blocks where hostile mobs can spawn: red = any time, yellow = at night. */
    private @Nullable TextRenderInfo spawnSpot(BlockPos pos, BlockState state, BlockPos abovePos, BlockState above) {
        if (!above.getFluidState().isEmpty() || above.is(BlockTags.PREVENT_MOB_SPAWNING_INSIDE)) {
            return null;
        }
        if (!state.isValidSpawn(getClient().level, pos, EntityTypes.ZOMBIE)) {
            return null;
        }
        int blockLight = getClient().level.getBrightness(LightLayer.BLOCK, abovePos);
        int skyLight = getClient().level.getBrightness(LightLayer.SKY, abovePos);
        return switch (SpawnLight.classify(blockLight, skyLight)) {
            case SAFE -> null;
            case NIGHT -> new TextRenderInfo("×", 0xFFD54F);
            case ALWAYS -> new TextRenderInfo("×", 0xFF3B30);
        };
    }

    @Override
    public List<String> getSettingsDisplay() {
        List<String> settings = new ArrayList<>();
        settings.add("Toggle: " + (isEnabled() ? "ON" : "OFF"));
        settings.add("XZ radius : " + XZ_RADIUS);
        settings.add("Y radius : " + Y_RADIUS);
        settings.add("X+");
        settings.add("X-");
        settings.add("Y+");
        settings.add("Y-");
        settings.add("Mode: " + (spawnMode ? "Spawn spots (red = any time, yellow = night)" : "Light levels"));

        return settings;
    }

    @Override
    public void onSettingSelected(int settingIndex) {
        switch (settingIndex) {
            case 0 -> toggleEnabled();
            case 3 -> XZ_RADIUS = Math.min(64, XZ_RADIUS + 1);
            case 4 -> XZ_RADIUS = Math.max(1, XZ_RADIUS - 1);
            case 5 -> Y_RADIUS = Math.min(32, Y_RADIUS + 1);
            case 6 -> Y_RADIUS = Math.max(1, Y_RADIUS - 1);
            case 7 -> spawnMode = !spawnMode;
            default -> {
                // Nothing
            }
        }
    }

    @Override
    public JsonObject saveSettings() {
        JsonObject json = new JsonObject();
        json.addProperty("xzRadius", XZ_RADIUS);
        json.addProperty("yRadius", Y_RADIUS);
        json.addProperty("spawnMode", spawnMode);
        return json;
    }

    @Override
    public void loadSettings(JsonObject settings) {
        XZ_RADIUS = PersistentSettings.readInt(settings, "xzRadius", XZ_RADIUS, 1, 64);
        Y_RADIUS = PersistentSettings.readInt(settings, "yRadius", Y_RADIUS, 1, 32);
        spawnMode = PersistentSettings.readBoolean(settings, "spawnMode", spawnMode);
    }

    @Override
    protected int getXZRadius() {
        return XZ_RADIUS;
    }

    @Override
    protected int getYRadius() {
        return Y_RADIUS;
    }
}
