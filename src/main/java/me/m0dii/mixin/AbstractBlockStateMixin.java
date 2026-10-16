package me.m0dii.mixin;

import me.m0dii.utils.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class AbstractBlockStateMixin {

    @Unique
    private static final ThreadLocal<Boolean> m0dev$collisionOutlineGuard = ThreadLocal.withInitial(() -> false);

    @Shadow
    public abstract VoxelShape getCollisionShape(BlockGetter world, BlockPos pos, CollisionContext context);

    @Shadow
    public abstract Block getBlock();

    @Inject(
            method = "getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void useCollisionShapeOutline(BlockGetter world, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        Block block = getBlock();
        if (ModConfig.blockAttributesShowLightBlocks && block == Blocks.LIGHT) {
            cir.setReturnValue(Shapes.block());
            return;
        }
        if (ModConfig.blockAttributesShowBarrierBlocks && block == Blocks.BARRIER) {
            cir.setReturnValue(Shapes.block());
            return;
        }

        if (!ModConfig.blockAttributesShowCollisionMesh || m0dev$collisionOutlineGuard.get()) {
            return;
        }

        m0dev$collisionOutlineGuard.set(true);
        try {
            VoxelShape collision = getCollisionShape(world, pos, context);
            if (collision.isEmpty()) {
                return;
            }

            // Keep normal behavior for full-cube blocks (stone/dirt/etc.) and only expose detailed collision meshes.
            if (Shapes.joinIsNotEmpty(collision, Shapes.block(), BooleanOp.NOT_SAME)) {
                cir.setReturnValue(collision);
            }
        } finally {
            m0dev$collisionOutlineGuard.set(false);
        }
    }
}

