package me.m0dii.mixin;

import me.m0dii.modules.mobai.MobAiVisualizerModule;
import net.minecraft.client.network.ClientDebugSubscriptionManager;
import net.minecraft.world.debug.DebugSubscriptionType;
import net.minecraft.world.debug.DebugSubscriptionTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(ClientDebugSubscriptionManager.class)
public class ClientDebugSubscriptionManagerMixin {

    @Inject(method = "getRequestedSubscriptions", at = @At("RETURN"), cancellable = true)
    private void m0dev$addMobAiSubscriptions(CallbackInfoReturnable<Set<DebugSubscriptionType<?>>> cir) {
        if (!MobAiVisualizerModule.INSTANCE.shouldRequestServerDebugData()) {
            return;
        }

        Set<DebugSubscriptionType<?>> subscriptions = cir.getReturnValue();
        subscriptions.add(DebugSubscriptionTypes.ENTITY_PATHS);
        subscriptions.add(DebugSubscriptionTypes.BRAINS);
        cir.setReturnValue(subscriptions);
    }
}
