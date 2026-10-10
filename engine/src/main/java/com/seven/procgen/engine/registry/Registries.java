package com.seven.procgen.engine.registry;

public final class Registries {

    private static <T> RegistryKey<Registry<T>> create(String id) {
        return RegistryKey.ofRegistry(Identifier.of(id));
    }
}
