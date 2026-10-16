package me.m0dii.mixin;

import me.m0dii.modules.mobai.MobAiVisualizerModule;
import net.minecraft.client.multiplayer.ClientDebugSubscriber;
import net.minecraft.util.debug.DebugSubscription;
import net.minecraft.util.debug.DebugSubscriptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(ClientDebugSubscriber.class)
public class ClientDebugSubscriptionManagerMixin {

    @Inject(method = "requestedSubscriptions", at = @At("RETURN"), cancellable = true)
    private void m0dev$addMobAiSubscriptions(CallbackInfoReturnable<Set<DebugSubscription<?>>> cir) {
        if (!MobAiVisualizerModule.INSTANCE.shouldRequestServerDebugData()) {
            return;
        }

        Set<DebugSubscription<?>> subscriptions = cir.getReturnValue();
        subscriptions.add(DebugSubscriptions.ENTITY_PATHS);
        subscriptions.add(DebugSubscriptions.BRAINS);
        cir.setReturnValue(subscriptions);
    }
}
