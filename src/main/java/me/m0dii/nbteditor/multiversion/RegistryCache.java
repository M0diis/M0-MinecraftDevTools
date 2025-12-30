package me.m0dii.nbteditor.multiversion;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public class RegistryCache {

    private static final Map<DynamicRegistryManager, RegistryCache> caches = Collections.synchronizedMap(new WeakHashMap<>());

    private static final LoadingCache<Registry<?>, Boolean> staticRegistries = CacheBuilder.newBuilder().build(
            CacheLoader.from(registry -> Registries.REGISTRIES.get(registry.getKey().getValue()) != null));

    private final WeakReference<DynamicRegistryManager> registryManagerRef;
    @SuppressWarnings("unused") // Holds a strong reference
    private final DynamicRegistryManager registryManager;
    private final Map<Identifier, Optional<? extends Registry<?>>> cache;

    public RegistryCache(DynamicRegistryManager registryManager, boolean stronglyRef) {
        this.registryManagerRef = new WeakReference<>(registryManager);
        this.registryManager = (stronglyRef ? registryManager : null);
        this.cache = new ConcurrentHashMap<>();
    }

    public RegistryCache(DynamicRegistryManager registryManager) {
        this(registryManager, true);
    }

    public static RegistryCache get(DynamicRegistryManager registryManager) {
        return caches.computeIfAbsent(registryManager, key -> new RegistryCache(registryManager, false));
    }

    @Nullable
    public static <T> RegistryEntry.Reference<T> convertManagerWithCache(RegistryEntry.Reference<T> ref) {
        RegistryCache cache = get(DynamicRegistryManagerHolder.getManager());

        @SuppressWarnings("unchecked")
        Registry<T> registry = (Registry<T>) cache.getRegistry(ref.registryKey().getRegistry()).orElse(null);
        if (registry == null) {
            return null;
        }

        return registry.getEntry(ref.registryKey().getValue()).orElse(null);
    }

    public static boolean isRegistryStatic(Registry<?> registry) {
        return staticRegistries.getUnchecked(registry);
    }

    public Optional<? extends Registry<?>> getRegistry(Identifier registryKey) {
        return cache.computeIfAbsent(registryKey, id -> {
            DynamicRegistryManager manager = registryManagerRef.get();
            if (manager == null) {
                return Optional.empty();
            }
            return manager.getOptional(RegistryKey.ofRegistry(id));
        });
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<? extends Registry<T>> getRegistry(RegistryKey<Registry<T>> registryKey) {
        return (Optional<? extends Registry<T>>) getRegistry(registryKey.getValue());
    }

}
