package me.m0dii.nbteditor.mixin;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import me.m0dii.M0DevTools;
import me.m0dii.nbteditor.misc.MixinLink;
import net.minecraft.util.thread.ThreadExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.WeakHashMap;

@Mixin(ThreadExecutor.class)
public class ThreadExecutorMixin {
    private static final Cache<Runnable, Exception> stackTraces = CacheBuilder.newBuilder().weakKeys().build();
    private final WeakHashMap<Thread, Runnable> executeTask_task = new WeakHashMap<>();

    @Inject(method = "send", at = @At("HEAD"))
    @Group(name = "send", min = 1)
    private void send(Runnable runnable, CallbackInfo info) {
        stackTraces.put(runnable, new Exception("Stack trace"));
    }

    @Inject(method = "executeTask", at = @At("HEAD"))
    private void executeTask(Runnable task, CallbackInfo info) {
        executeTask_task.put(Thread.currentThread(), task);
    }

    @ModifyVariable(method = "executeTask", at = @At("STORE"))
    private Exception executeTask(Exception exception) {
        Runnable task = executeTask_task.remove(Thread.currentThread());
        Exception stackTrace = stackTraces.getIfPresent(task);
        if (stackTrace == null) {
            M0DevTools.LOGGER.warn("Missing additional #executeTask stack trace for exception");
        } else {
            exception.addSuppressed(stackTrace);
        }
        if (MixinLink.CATCH_BYPASSING_TASKS.remove(task) != null) {
            if (exception instanceof RuntimeException e) {
                throw e;
            }

            throw new RuntimeException("Failed to execute crashable task", exception); // Should never happen
        }
        return exception;
    }
}
