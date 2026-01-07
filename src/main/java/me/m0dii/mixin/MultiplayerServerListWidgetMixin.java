package me.m0dii.mixin;


import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

// Faster server pinging
@Mixin(MultiplayerServerListWidget.class)
public class MultiplayerServerListWidgetMixin {

    @Mutable
    @Final
    @Shadow
    static ThreadPoolExecutor SERVER_PINGER_THREAD_POOL;

    @Unique
    private static final int MIN_THREADS = 5;

    @Final
    @Shadow
    private List<MultiplayerServerListWidget.ServerEntry> servers;

    @Unique
    private static boolean poolReady = false;

    @Inject(method = "updateEntries", at = @At("HEAD"))
    private void updateEntriesInject(CallbackInfo ci) {
        if (!poolReady) {
            poolReady = true;
            refresh();
        }

        if (SERVER_PINGER_THREAD_POOL.getActiveCount() >= MIN_THREADS) {
            refresh();
        }
    }

    @Unique
    private void refresh() {
        SERVER_PINGER_THREAD_POOL.shutdownNow();
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("Pinger-#%d")
                .setDaemon(true)
                .build();

        SERVER_PINGER_THREAD_POOL = new ScheduledThreadPoolExecutor(servers.size() + MIN_THREADS, threadFactory);
    }
}