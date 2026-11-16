package me.m0dii.modules.waypoints;

import java.nio.file.Path;

/** Stable save identity, independent of the world's editable display name. */
public final class WorldKeys {
    private WorldKeys() { }

    public static String singleplayer(Path saveRoot) {
        return "sp-save:" + saveRoot.toAbsolutePath().normalize().toUri().toASCIIString();
    }
}
