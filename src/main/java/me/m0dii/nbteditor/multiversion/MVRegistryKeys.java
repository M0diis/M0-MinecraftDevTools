package me.m0dii.nbteditor.multiversion;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;

public class MVRegistryKeys {

    private MVRegistryKeys() {
    }

    public static final RegistryKey<Registry<World>> WORLD = RegistryKeys.WORLD;

}
