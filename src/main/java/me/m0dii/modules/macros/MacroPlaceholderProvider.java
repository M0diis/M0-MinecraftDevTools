package me.m0dii.modules.macros;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

import java.util.List;

public interface MacroPlaceholderProvider {
    String getProviderId();

    List<String> getPlaceholderDocs();

    String resolvePlaceholder(String token, MinecraftClient client, PlayerEntity player, boolean canvasMode);
}

