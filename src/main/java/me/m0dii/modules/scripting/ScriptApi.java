package me.m0dii.modules.scripting;

import me.m0dii.M0DevTools;
import me.m0dii.modules.macros.CommandMacros;
import me.m0dii.modules.macros.MacroPlaceholders;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * The {@code m0} helper available to every script.
 * <pre>
 * m0.message("hello")                 // local chat line (only you see it)
 * m0.actionbar("hello")               // action bar text
 * m0.chat("hello everyone")           // sends a chat message
 * m0.command("time set day")          // sends a command (leading '/' optional)
 * m0.placeholders("{player.x}")       // expands macro placeholders
 * m0.macro("my_macro")                // runs a saved macro
 * m0.sleep(500)                       // @async scripts only
 * m0.sync { player.jumpFromGround() } // run on the client thread (use from @async scripts)
 * def hp = m0.call { player.health }  // same, returning a value
 * m0.checkCancelled()                 // call in long loops so /runscript stop and timeouts work
 * </pre>
 * All chat/command/message helpers are safe to call from {@code @async} scripts; they hop to the client thread.
 */
public final class ScriptApi {
    private static final long MAIN_THREAD_WAIT_SLICE_MS = 50L;

    private final ScriptJob job;

    ScriptApi(ScriptJob job) {
        this.job = job;
    }

    // ---- info -------------------------------------------------------------------------------------------------

    public boolean isAsync() {
        return job.isAsync();
    }

    public String scriptName() {
        return job.name();
    }

    public Minecraft client() {
        return Minecraft.getInstance();
    }

    public LocalPlayer player() {
        return Minecraft.getInstance().player;
    }

    public ClientLevel world() {
        return Minecraft.getInstance().level;
    }

    // ---- output -----------------------------------------------------------------------------------------------

    public void message(Object text) {
        String value = String.valueOf(text);
        sync(() -> {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.sendSystemMessage(Component.literal(value));
            }
        });
    }

    public void actionbar(Object text) {
        String value = String.valueOf(text);
        sync(() -> {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.sendOverlayMessage(Component.literal(value));
            }
        });
    }

    public void chat(Object text) {
        String value = String.valueOf(text);
        if (value.isBlank()) {
            return;
        }
        sync(() -> {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.connection.sendChat(value);
            }
        });
    }

    public void command(Object text) {
        String value = String.valueOf(text).trim();
        if (value.startsWith("/")) {
            value = value.substring(1);
        }
        if (value.isBlank()) {
            return;
        }
        String command = value;
        sync(() -> {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.connection.sendCommand(command);
            }
        });
    }

    public void log(Object text) {
        M0DevTools.LOGGER.info("[script {}] {}", job.name(), text);
    }

    // ---- macros -----------------------------------------------------------------------------------------------

    public String placeholders(Object text) {
        String value = String.valueOf(text);
        return call(() -> MacroPlaceholders.expand(Minecraft.getInstance(), value));
    }

    public boolean macro(String macroId) {
        return Boolean.TRUE.equals(call(() -> CommandMacros.runMacroById(macroId)));
    }

    // ---- flow control -----------------------------------------------------------------------------------------

    public boolean isCancelled() {
        return job.isCancelled();
    }

    public void checkCancelled() {
        job.checkCancelled();
    }

    /** Pauses an {@code @async} script. Not allowed on the client thread, where it would freeze the game. */
    public void sleep(long millis) {
        if (ScriptRunner.isOnMainThread()) {
            throw new ScriptExecutionException("m0.sleep() freezes the game on the client thread; add '// @async on' to the script.");
        }
        long deadline = System.currentTimeMillis() + Math.max(0L, millis);
        while (true) {
            job.checkCancelled();
            long left = deadline - System.currentTimeMillis();
            if (left <= 0L) {
                return;
            }
            try {
                Thread.sleep(Math.min(left, MAIN_THREAD_WAIT_SLICE_MS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                job.checkCancelled();
                throw new ScriptExecutionException("Script '" + job.name() + "' was interrupted.");
            }
        }
    }

    /** Runs {@code action} on the client thread and waits for it. Runs directly when already on it. */
    public void sync(Runnable action) {
        call(() -> {
            action.run();
            return null;
        });
    }

    /** Runs {@code action} on the client thread and returns its value. Runs directly when already on it. */
    public <T> T call(Supplier<T> action) {
        if (ScriptRunner.isOnMainThread()) {
            return action.get();
        }
        CompletableFuture<T> future = new CompletableFuture<>();
        ScriptRunner.runOnMainThread(() -> {
            try {
                future.complete(action.get());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        while (true) {
            job.checkCancelled();
            try {
                return future.get(MAIN_THREAD_WAIT_SLICE_MS, TimeUnit.MILLISECONDS);
            } catch (TimeoutException ignored) {
                // keep waiting, but stay responsive to cancellation
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                job.checkCancelled();
                throw new ScriptExecutionException("Script '" + job.name() + "' was interrupted.");
            } catch (ExecutionException e) {
                Throwable cause = e.getCause() == null ? e : e.getCause();
                if (cause instanceof RuntimeException runtime) {
                    throw runtime;
                }
                throw new ScriptExecutionException("Client-thread call failed: " + cause, cause);
            }
        }
    }

    @Override
    public String toString() {
        return "m0 (script helper; see ScriptApi for methods)";
    }
}
