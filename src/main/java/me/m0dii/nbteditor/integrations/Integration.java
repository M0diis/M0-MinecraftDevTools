package me.m0dii.nbteditor.integrations;

import net.fabricmc.loader.api.FabricLoader;

import java.util.Optional;
import java.util.function.Supplier;

public abstract class Integration {

    private Boolean loaded;

    public static <T extends Integration> Optional<T> getOptional(Supplier<T> integration) {
        T value = integration.get();

        if (value.isLoaded()) {
            return Optional.of(value);
        }

        return Optional.empty();
    }

    public boolean isLoaded() {
        if (loaded == null) {
            loaded = FabricLoader.getInstance().getAllMods().stream()
                    .anyMatch(mod -> mod.getMetadata().getId().equals(getModId()));
        }
        return loaded;
    }

    public abstract String getModId();

}
