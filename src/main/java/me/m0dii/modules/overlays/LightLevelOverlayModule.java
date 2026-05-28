package me.m0dii.modules.overlays;

import me.m0dii.utils.KeybindCatalog;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LightLevelOverlayModule extends BlockTextOverlayModule {

    private int XZ_RADIUS = 16;
    private int Y_RADIUS = 4;

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
                InputUtil.Type.KEYSYM,
                KeybindCatalog.LIGHT_OVERLAY_TOGGLE.defaultKey(),
                client -> toggleEnabled());
    }

    @Nullable
    @Override
    protected TextRenderInfo shouldRender(BlockPos pos, Vec3d cameraPos) {
        BlockState state = getClient().world.getBlockState(pos);
        if (state.isAir() || state.getLuminance() > 0 || excludedBlocks.contains(state.getBlock())) {
            return null;
        }

        BlockState above = getClient().world.getBlockState(pos.up());
        boolean topExposed = above.isAir() || above.getCollisionShape(getClient().world, pos.up()).isEmpty();
        if (!topExposed) {
            return null;
        }

        int light = getClient().world.getLightLevel(pos.up());
        if (light <= 0) {
            return null;
        }

        int color = (light < 8) ? 0xFF0000 : 0x00FF00;
        return new TextRenderInfo(Integer.toString(light), color);
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

        return settings;
    }

    @Override
    public void onSettingSelected(int settingIndex) {
        switch (settingIndex) {
            case 0 -> toggleEnabled();
            case 3 -> XZ_RADIUS++;
            case 4 -> XZ_RADIUS = Math.max(1, XZ_RADIUS - 1);
            case 5 -> Y_RADIUS++;
            case 6 -> Y_RADIUS = Math.max(1, Y_RADIUS - 1);
            default -> {
                // Nothing
            }
        }
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
