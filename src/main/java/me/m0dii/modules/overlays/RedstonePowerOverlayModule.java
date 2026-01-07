package me.m0dii.modules.overlays;

import me.m0dii.utils.ModConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class RedstonePowerOverlayModule extends BlockTextOverlayModule {

    public static final RedstonePowerOverlayModule INSTANCE = new RedstonePowerOverlayModule();

    private RedstonePowerOverlayModule() {
        super("redstone_overlay", "Redstone Overlay", false);
    }

    @Override
    public void register() {
        super.register();

        registerPressedKeybind("key.m0-dev-tools.toggle_redstone_overlay",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F8,
                client -> toggleEnabled());
    }

    @Nullable
    @Override
    protected TextRenderInfo shouldRender(BlockPos pos, Vec3d cameraPos) {
        BlockState state = getClient().world.getBlockState(pos);
        if (state.isAir()) {
            return null;
        }

        if (!(state.getBlock() instanceof RedstoneWireBlock)) {
            return null;
        }

        int wirePower;
        try {
            wirePower = state.get(RedstoneWireBlock.POWER);
        } catch (Exception e) {
            return null;
        }
        if (wirePower <= 0) {
            return null;
        }

        int g = (int) Math.round((wirePower / 15.0) * 255.0);
        int color = (0xFF << 16) | (g << 8);

        return new TextRenderInfo(Integer.toString(wirePower), color, 0.5, 0.05, 0.35);
    }

    @Override
    protected int getXZRadius() {
        return ModConfig.overlayXZradius;
    }

    @Override
    protected int getYRadius() {
        return ModConfig.overlayYradius;
    }
}
