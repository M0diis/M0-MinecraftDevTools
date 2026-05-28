package me.m0dii.mixin;

import me.m0dii.modules.heldlight.HeldLightConfigDataHandler;
import me.m0dii.modules.heldlight.HeldLightModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererHeldLightMixin {

    @Inject(method = "getLightmapCoordinates(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/util/math/BlockPos;)I", at = @At("RETURN"), cancellable = true)
    private static void injectHeldLight(BlockRenderView world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (!HeldLightModule.INSTANCE.isEnabled()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.world == null || client.world != world) {
            return;
        }

        ItemStack mainHand = client.player.getMainHandStack();
        ItemStack offHand = client.player.getOffHandStack();
        if (!HeldLightModule.isLightSource(mainHand) && !HeldLightModule.isLightSource(offHand)) {
            return;
        }

        HeldLightConfigDataHandler.Config cfg = HeldLightConfigDataHandler.get();
        double dx = pos.getX() + 0.5 - client.player.getX();
        double dy = pos.getY() + 0.5 - (client.player.getY() + client.player.getStandingEyeHeight());
        double dz = pos.getZ() + 0.5 - client.player.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance > cfg.falloffDistance) {
            return;
        }

        float distanceFactor = (float) Math.clamp(1.0 - (distance / cfg.falloffDistance), 0.0, 1.0);
        int heldBlockLight = Math.max(1, Math.round(cfg.brightness * distanceFactor));
        int packed = cir.getReturnValue();
        int currentBlock = (packed >> 4) & 0xF;
        if (heldBlockLight <= currentBlock) {
            return;
        }

        int sky = (packed >> 20) & 0xF;
        cir.setReturnValue((heldBlockLight << 4) | (sky << 20));
    }
}

