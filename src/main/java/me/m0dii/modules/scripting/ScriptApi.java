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
 * m0.print("hello")                   // line in the script editor's Console tab
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

    /** A line in the script editor's Console tab (and the log). */
    public void print(Object text) {
        String value = String.valueOf(text);
        ScriptConsole.add(ScriptConsole.Level.INFO, value);
        M0DevTools.LOGGER.info("[script {}] {}", job.name(), value);
    }

    public void log(Object text) {
        M0DevTools.LOGGER.info("[script {}] {}", job.name(), text);
    }

    /** One-line help for each helper, shown next to the editor's completion suggestions. */
    public static String describe(String method) {
        return switch (method) {
            case "print" -> "line in the Console tab";
            case "log" -> "write to the game log";
            case "message" -> "chat line only you see";
            case "actionbar" -> "action bar text";
            case "chat" -> "send a chat message";
            case "command" -> "send a command ('/' optional)";
            case "placeholders" -> "expand {placeholders}";
            case "macro" -> "run a saved macro by id";
            case "sleep" -> "wait (async scripts only)";
            case "sync" -> "run on the game thread";
            case "call" -> "run on the game thread, return value";
            case "checkCancelled" -> "stop here if cancelled";
            case "isCancelled" -> "true after stop/timeout";
            case "isAsync" -> "true in @async scripts";
            case "scriptName" -> "this script's file name";
            case "client", "player", "world" -> "Minecraft " + method;
            default -> "";
        };
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
