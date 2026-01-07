package me.m0dii.modules.overlays;

import me.m0dii.modules.BlockTargetTextModule;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.CommandBlockExecutor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public class CommandBlockOverlayModule extends BlockTargetTextModule {

    public static final CommandBlockOverlayModule INSTANCE = new CommandBlockOverlayModule();

    protected CommandBlockOverlayModule() {
        super("command_block_overlay", "Command Block Overlay", false);
    }

    @Override
    public void register() {
        super.register();

        registerPressedKeybind(
                "key.m0-dev-tools.command_block_overlay",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_X,
                client -> toggleEnabled()
        );
    }

    @Override
    protected boolean shouldRenderFor(@NotNull BlockPos pos, @NotNull BlockState blockState) {
        return getWorld().getBlockEntity(pos) instanceof CommandBlockBlockEntity;
    }

    @Override
    protected void renderText(@NotNull MatrixStack matrices, @NotNull Camera camera, @NotNull BlockPos pos, @NotNull BlockState blockState) {
        BlockEntity blockEntity = getWorld().getBlockEntity(pos);
        if (!(blockEntity instanceof CommandBlockBlockEntity commandBlock)) {
            return;
        }

        CommandBlockExecutor executor = commandBlock.getCommandExecutor();
        String command = executor.getCommand();

        if (command == null || command.isEmpty()) {
            command = "[Empty]";
        }

        Vec3d renderPos = getBlockCenterWithVerticalOffset(pos, 0.75);
        renderFloatingText(matrices, renderPos, camera.getPos(), command, camera);
    }
}
