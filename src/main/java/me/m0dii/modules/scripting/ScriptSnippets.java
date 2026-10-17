package me.m0dii.modules.scripting;

import java.util.List;
import java.util.Locale;

/** Ready-made code for the script editor's snippet palette (Ctrl+J). Bodies use 4-space indents. */
public final class ScriptSnippets {
    public record Snippet(String label, String detail, String body) {
    }

    private ScriptSnippets() {
    }

    /** {@code language} is "groovy", "kotlin" or "javascript". */
    public static List<Snippet> forLanguage(String language) {
        return switch (language == null ? "" : language.toLowerCase(Locale.ROOT)) {
            case "kotlin" -> KOTLIN;
            case "javascript" -> JAVASCRIPT;
            default -> GROOVY;
        };
    }

    private static final List<Snippet> GROOVY = List.of(
            new Snippet("Nearby entities", "loop over entities within 16 blocks", """
                    def box = player.getBoundingBox().inflate(16)
                    world.getEntities(player, box).each { entity ->
                        m0.print("${entity.getName().getString()} at ${entity.blockPosition()}")
                    }"""),
            new Snippet("Scan blocks around me", "count a block type in a cube", """
                    def center = player.blockPosition()
                    int found = 0
                    for (int x = -8; x <= 8; x++) {
                        for (int y = -4; y <= 4; y++) {
                            for (int z = -8; z <= 8; z++) {
                                def state = world.getBlockState(center.offset(x, y, z))
                                if (state.getBlock().toString().contains("diamond_ore")) {
                                    found++
                                }
                            }
                        }
                    }
                    m0.message("Found ${found} diamond ore")"""),
            new Snippet("Repeat with delay (async)", "needs // @async on at the top", """
                    // @async on
                    for (int i = 1; i <= 5; i++) {
                        m0.checkCancelled()
                        m0.actionbar("Step ${i}/5")
                        m0.sleep(1000)
                    }"""),
            new Snippet("Held item info", "id, count and components", """
                    def stack = player.getMainHandItem()
                    m0.print("${stack.getItem()} x${stack.getCount()}")
                    m0.print(stack.getComponents().toString())"""),
            new Snippet("Run commands", "send several commands", """
                    ["time set day", "weather clear"].each { cmd ->
                        m0.command(cmd)
                    }"""),
            new Snippet("Position and look", "where you are and where you face", """
                    m0.message(String.format("%.1f %.1f %.1f  yaw %.0f  pitch %.0f",
                            player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()))""")
    );

    private static final List<Snippet> KOTLIN = List.of(
            new Snippet("Nearby entities", "loop over entities within 16 blocks", """
                    val box = player!!.boundingBox.inflate(16.0)
                    for (entity in world!!.getEntities(player, box)) {
                        m0.print("${entity.name.string} at ${entity.blockPosition()}")
                    }"""),
            new Snippet("Repeat with delay (async)", "needs // @async on at the top", """
                    // @async on
                    for (i in 1..5) {
                        m0.checkCancelled()
                        m0.actionbar("Step $i/5")
                        m0.sleep(1000)
                    }"""),
            new Snippet("Held item info", "id and count", """
                    val stack = player!!.mainHandItem
                    m0.print("${stack.item} x${stack.count}")"""),
            new Snippet("Run commands", "send several commands", """
                    listOf("time set day", "weather clear").forEach { m0.command(it) }""")
    );

    private static final List<Snippet> JAVASCRIPT = List.of(
            new Snippet("Nearby entities", "loop over entities within 16 blocks", """
                    const box = player.getBoundingBox().inflate(16);
                    for (const entity of world.getEntities(player, box)) {
                        m0.print(entity.getName().getString() + " at " + entity.blockPosition());
                    }"""),
            new Snippet("Repeat with delay (async)", "needs // @async on at the top", """
                    // @async on
                    for (let i = 1; i <= 5; i++) {
                        m0.checkCancelled();
                        m0.actionbar("Step " + i + "/5");
                        m0.sleep(1000);
                    }"""),
            new Snippet("Run commands", "send several commands", """
                    ["time set day", "weather clear"].forEach(cmd => m0.command(cmd));""")
    );

    /** Indents every line after the first so a snippet lines up with the cursor's current indentation. */
    public static String indented(String body, String indent) {
        if (indent == null || indent.isEmpty()) {
            return body;
        }
        return body.replace("\n", "\n" + indent);
    }
}
