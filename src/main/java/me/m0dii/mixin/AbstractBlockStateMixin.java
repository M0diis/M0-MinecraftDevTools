package me.m0dii.mixin;

import me.m0dii.utils.ModConfig;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class AbstractBlockStateMixin {

    @Unique
    private static final ThreadLocal<Boolean> m0dev$collisionOutlineGuard = ThreadLocal.withInitial(() -> false);

    @Shadow
    public abstract VoxelShape getCollisionShape(BlockView world, BlockPos pos, ShapeContext context);

    @Shadow
    public abstract Block getBlock();

    @Inject(
            method = "getOutlineShape(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/ShapeContext;)Lnet/minecraft/util/shape/VoxelShape;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void useCollisionShapeOutline(BlockView world, BlockPos pos, ShapeContext context, CallbackInfoReturnable<VoxelShape> cir) {
        Block block = getBlock();
        if (ModConfig.blockAttributesShowLightBlocks && block == Blocks.LIGHT) {
            cir.setReturnValue(VoxelShapes.fullCube());
            return;
        }
        if (ModConfig.blockAttributesShowBarrierBlocks && block == Blocks.BARRIER) {
            cir.setReturnValue(VoxelShapes.fullCube());
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
            if (VoxelShapes.matchesAnywhere(collision, VoxelShapes.fullCube(), BooleanBiFunction.NOT_SAME)) {
                cir.setReturnValue(collision);
            }
        } finally {
            m0dev$collisionOutlineGuard.set(false);
        }
    }
}

