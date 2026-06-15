package me.m0dii.modules.worldedit;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.RootCommandNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldEditCommandDetectionTest {
    @Test
    void detectsDoubleSlashAliasStyleCommands() {
        RootCommandNode<Object> root = new RootCommandNode<>();
        root.addChild(LiteralArgumentBuilder.literal("/set").build());
        root.addChild(LiteralArgumentBuilder.literal("/replace").build());
        root.addChild(LiteralArgumentBuilder.literal("/pos1").build());
        root.addChild(LiteralArgumentBuilder.literal("/pos2").build());
        root.addChild(LiteralArgumentBuilder.literal("/wand").build());

        assertTrue(WorldEditClientCommands.hasNativeWorldEditCommandSignature(root));
    }

    @Test
    void detectsServerWorldEditRootAlias() {
        RootCommandNode<Object> root = new RootCommandNode<>();
        root.addChild(LiteralArgumentBuilder.literal("worldedit").build());

        assertTrue(WorldEditClientCommands.hasNativeWorldEditCommandSignature(root));
    }

    @Test
    void requiresCompleteSignatureWhenNoRootAliasExists() {
        RootCommandNode<Object> root = new RootCommandNode<>();
        root.addChild(LiteralArgumentBuilder.literal("/set").build());
        root.addChild(LiteralArgumentBuilder.literal("/replace").build());
        root.addChild(LiteralArgumentBuilder.literal("/pos1").build());
        root.addChild(LiteralArgumentBuilder.literal("/wand").build());

        assertFalse(WorldEditClientCommands.hasNativeWorldEditCommandSignature(root));
    }
}
