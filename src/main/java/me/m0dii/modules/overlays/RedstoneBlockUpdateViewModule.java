package me.m0dii.modules.overlays;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import me.m0dii.modules.BlockTargetTextModule;
import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Set;

public class RedstoneBlockUpdateViewModule extends BlockTargetTextModule {

    public static final RedstoneBlockUpdateViewModule INSTANCE = new RedstoneBlockUpdateViewModule();

    protected RedstoneBlockUpdateViewModule() {
        super("redstone_bud_order", "Redstone Update Order", false);
    }

    @Override
    public void register() {
        super.register();

        registerPressedKeybind(
                "key.m0-dev-tools.toggle_redstone_bud",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                client -> toggleEnabled()
        );
    }

    @Override
    protected boolean shouldRenderFor(@NotNull BlockPos pos, @NotNull BlockState blockState) {
        return blockState.getBlock() instanceof RedstoneWireBlock;
    }

    @Override
    protected void renderText(@NotNull MatrixStack matrices, @NotNull Camera camera, @NotNull BlockPos pos, @NotNull BlockState blockState) {
        List<BlockPos> order = getDustUpdateOrderAt(pos);

        for (int i = 0; i < order.size(); i++) {
            Vec3d renderPos = getBlockCenterWithVerticalOffset(order.get(i), 0);
            renderFloatingText(matrices, renderPos, camera.getPos(), String.valueOf(i + 1), camera);
        }
    }

    private static List<BlockPos> getDustUpdateOrderAt(BlockPos pos) {
        Set<BlockPos> set = Sets.newHashSet();
        set.add(pos);
        for (Direction direction : Direction.values()) {
            set.add(pos.offset(direction));
        }
        return Lists.newArrayList(set);
    }
}
