package me.m0dii.modules.overlays;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class LightLevelOverlayModule extends BlockTextOverlayModule {

    private static final int XZ_RADIUS = 16;
    private static final int Y_RADIUS = 4;

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

        registerPressedKeybind("key.m0-dev-tools.toggle_light_overlay",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F9,
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
    protected int getXZRadius() {
        return XZ_RADIUS;
    }

    @Override
    protected int getYRadius() {
        return Y_RADIUS;
    }
}
