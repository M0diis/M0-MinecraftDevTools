package me.m0dii.modules.scripting;

import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class ScriptTypes {
    public static final String GROOVY_EXT = ".groovy";
    public static final String KOTLIN_EXT = ".kts";
    public static final String JAVASCRIPT_EXT = ".js";

    private static final GroovyScriptManager GROOVY = new GroovyScriptManager();
    private static final KotlinScriptManager KOTLIN = new KotlinScriptManager();
    private static final JavaScriptScriptManager JAVASCRIPT = new JavaScriptScriptManager();

    private ScriptTypes() {
    }

    public static boolean isScriptFile(@NotNull String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(GROOVY_EXT) || lower.endsWith(KOTLIN_EXT) || lower.endsWith(JAVASCRIPT_EXT);
    }

    public static boolean isGroovy(@NotNull String name) {
        return name.toLowerCase(Locale.ROOT).endsWith(GROOVY_EXT);
    }

    public static boolean isKotlin(@NotNull String name) {
        return name.toLowerCase(Locale.ROOT).endsWith(KOTLIN_EXT);
    }

    public static boolean isJavaScript(@NotNull String name) {
        return name.toLowerCase(Locale.ROOT).endsWith(JAVASCRIPT_EXT);
    }

    public static @NotNull ScriptManager managerFor(@NotNull String name) {
        if (isGroovy(name)) {
            return GROOVY;
        }
        if (isKotlin(name)) {
            return KOTLIN;
        }
        if (isJavaScript(name)) {
            return JAVASCRIPT;
        }
        throw new IllegalArgumentException("Unsupported script type: " + name);
    }

    public static @NotNull Map<String, Object> defaultContext() {
        MinecraftClient client = MinecraftClient.getInstance();
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("client", client);
        context.put("source", client.player);
        context.put("player", client.player);
        context.put("world", client.world);
        context.put("options", client.options);
        context.put("server", client.getServer());
        return context;
    }

    public static @NotNull String formatResult(Object result) {
        if (result == null) {
            return "Script completed.";
        }
        String text = String.valueOf(result);
        return text == null || text.isBlank() || "null".equalsIgnoreCase(text.trim())
                ? "Script completed."
                : text;
    }
}
