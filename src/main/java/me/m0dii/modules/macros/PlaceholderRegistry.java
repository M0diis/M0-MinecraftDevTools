package me.m0dii.modules.macros;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class PlaceholderRegistry {
    @FunctionalInterface
    interface Resolver {
        String resolve(Context context);
    }

    record Context(MinecraftClient client,
                   PlayerEntity player,
                   Identifier dimensionId,
                   BlockPos lookBlock,
                   Entity lookEntity,
                   boolean canvasMode,
                   String token) {
    }

    private final Map<String, Resolver> exactResolvers = new LinkedHashMap<>();

    public PlaceholderRegistry register(String token, Resolver resolver) {
        if (token != null && !token.isBlank() && resolver != null) {
            exactResolvers.put(token, resolver);
        }
        return this;
    }

    public String resolveExact(Context context) {
        Resolver resolver = exactResolvers.get(context.token());
        if (resolver == null) {
            return null;
        }
        return resolver.resolve(context);
    }

    public Set<String> tokens() {
        return exactResolvers.keySet();
    }
}

