package me.m0dii.modules.overlays;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import me.m0dii.modules.BlockTargetTextModule;
import me.m0dii.utils.KeybindCatalog;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class CommandBlockOverlayModule extends BlockTargetTextModule {

    public static final CommandBlockOverlayModule INSTANCE = new CommandBlockOverlayModule();

    protected CommandBlockOverlayModule() {
        super("command_block_overlay", "Command Block Overlay", false);
    }

    @Override
    public void register() {
        super.register();

        registerPressedKeybind(
                KeybindCatalog.COMMAND_BLOCK_OVERLAY_TOGGLE.translationKey(),
                InputConstants.Type.KEYBOARD,
                KeybindCatalog.COMMAND_BLOCK_OVERLAY_TOGGLE.defaultKey(),
                client -> toggleEnabled()
        );
    }

    @Override
    protected boolean shouldRenderFor(@NotNull BlockPos pos, @NotNull BlockState blockState) {
        return getWorld().getBlockEntity(pos) instanceof CommandBlockEntity;
    }

    @Override
    protected void renderText(@NotNull PoseStack matrices, @NotNull Camera camera, @NotNull BlockPos pos, @NotNull BlockState blockState) {
        BlockEntity blockEntity = getWorld().getBlockEntity(pos);
        if (!(blockEntity instanceof CommandBlockEntity commandBlock)) {
            return;
        }

        BaseCommandBlock executor = commandBlock.getCommandBlock();
        String command = executor.getCommand();

        if (command == null || command.isEmpty()) {
            command = "[Empty]";
        }

        Vec3 renderPos = getBlockCenterWithVerticalOffset(pos, 0.75);
        renderFloatingText(matrices, renderPos, camera.position(), command, camera);
    }
}
