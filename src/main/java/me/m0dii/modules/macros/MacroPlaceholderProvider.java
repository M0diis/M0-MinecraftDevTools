package me.m0dii.modules.macros;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public interface MacroPlaceholderProvider {
    String getProviderId();

    List<String> getPlaceholderDocs();

    default List<String> getKnownPlaceholderTokens() {
        return List.of();
    }

    String resolvePlaceholder(String token, Minecraft client, Player player, boolean canvasMode);
}

