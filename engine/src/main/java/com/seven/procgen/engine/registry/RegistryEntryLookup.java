package com.seven.procgen.engine.registry;

import java.util.Optional;

public interface RegistryEntryLookup<T> extends RegistryEntryOwner<T> {
    Optional<RegistryEntry<T>> getOptional(RegistryKey<T> key);

    default RegistryEntry<T> getOrThrow(RegistryKey<T> key) {
        return this.getOptional(key).orElseThrow();
    }

    interface RegistryLookup {
        <T> Optional<RegistryEntryLookup<T>> getOptional(RegistryKey<? extends Registry<? extends T>> registryKey);

        default <T> Optional<RegistryEntry<T>> getOptionalEntry(RegistryKey<? extends Registry<? extends T>> registryKey, RegistryKey<T> key) {
            return this.getOptional(registryKey).flatMap(l -> l.getOptional(key));
        }

        default <T> RegistryEntryLookup<T> getOrThrow(RegistryKey<? extends Registry<? extends T>> registryKey) {
            return this.getOptional(registryKey).orElseThrow();
        }
    }
}
