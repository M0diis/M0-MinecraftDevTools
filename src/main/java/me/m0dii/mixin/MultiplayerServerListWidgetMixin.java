package me.m0dii.mixin;


import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

// Faster server pinging
@Mixin(ServerSelectionList.class)
public class MultiplayerServerListWidgetMixin {

    @Mutable
    @Final
    @Shadow
    static ThreadPoolExecutor THREAD_POOL;

    @Unique
    private static final int MIN_THREADS = 5;

    @Final
    @Shadow
    private List<ServerSelectionList.OnlineServerEntry> onlineServers;

    @Unique
    private static boolean poolReady = false;

    @Inject(method = "refreshEntries", at = @At("HEAD"))
    private void updateEntriesInject(CallbackInfo ci) {
        if (!poolReady) {
            poolReady = true;
            refresh();
        }

        if (THREAD_POOL.getActiveCount() >= MIN_THREADS) {
            refresh();
        }
    }

    @Unique
    private void refresh() {
        THREAD_POOL.shutdownNow();
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("Pinger-#%d")
                .setDaemon(true)
                .build();

        THREAD_POOL = new ScheduledThreadPoolExecutor(onlineServers.size() + MIN_THREADS, threadFactory);
    }
}