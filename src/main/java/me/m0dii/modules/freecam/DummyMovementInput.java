package me.m0dii.modules.freecam;

import net.minecraft.client.Options;
import net.minecraft.client.player.KeyboardInput;

public class DummyMovementInput extends KeyboardInput {
    public DummyMovementInput(Options options) {
        super(options);
    }

    @Override
    public void tick() {
        // No-op
    }
}
