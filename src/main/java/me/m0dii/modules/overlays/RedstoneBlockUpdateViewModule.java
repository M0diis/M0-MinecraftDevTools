package me.m0dii.modules.overlays;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import me.m0dii.modules.BlockTargetTextModule;
import me.m0dii.utils.KeybindCatalog;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RedstoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RedstoneBlockUpdateViewModule extends BlockTargetTextModule {

    public static final RedstoneBlockUpdateViewModule INSTANCE = new RedstoneBlockUpdateViewModule();

    protected RedstoneBlockUpdateViewModule() {
        super("redstone_bud_order", "Redstone Update Order", false);
    }

    @Override
    public void register() {
        super.register();

        registerPressedKeybind(
                KeybindCatalog.REDSTONE_BUD_TOGGLE.translationKey(),
                InputConstants.Type.KEYBOARD,
                KeybindCatalog.REDSTONE_BUD_TOGGLE.defaultKey(),
                client -> toggleEnabled()
        );
    }

    @Override
    protected boolean shouldRenderFor(@NotNull BlockPos pos, @NotNull BlockState blockState) {
        return blockState.getBlock() instanceof RedstoneWireBlock;
    }

    @Override
    protected void renderText(@NotNull PoseStack matrices, @NotNull Camera camera, @NotNull BlockPos pos, @NotNull BlockState blockState) {
        List<BlockPos> order = getDustUpdateOrderAt(pos);

        for (int i = 0; i < order.size(); i++) {
            Vec3 renderPos = getBlockCenterWithVerticalOffset(order.get(i), 0);
            renderFloatingText(matrices, renderPos, camera.position(), String.valueOf(i + 1), camera);
        }
    }

    private static List<BlockPos> getDustUpdateOrderAt(BlockPos pos) {
        List<BlockPos> ordered = Lists.newArrayList();
        ordered.add(pos);
        ordered.add(pos.west());
        ordered.add(pos.east());
        ordered.add(pos.below());
        ordered.add(pos.above());
        ordered.add(pos.north());
        ordered.add(pos.south());
        return ordered;
    }
}
