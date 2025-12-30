package me.m0dii.modules.freecam;

import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.option.GameOptions;

public class DummyMovementInput extends KeyboardInput {
    public DummyMovementInput(GameOptions options) {
        super(options);
    }

    @Override
    public void tick() {
        // No-op
    }
}
