package com.seven.procgen.engine.registry;

public final class RegistryKey<T> {
    private final Identifier registry;
    private final Identifier value;

    private RegistryKey(Identifier registry, Identifier value) {
        this.registry = registry;
        this.value = value;
    }

    private static <T> RegistryKey<T> of(Identifier registry, Identifier value) {
        return new RegistryKey<>(registry, value);
    }

    public static <T> RegistryKey<T> of(RegistryKey<? extends Registry<T>> registryKey, Identifier value) {
        return of(registryKey.value, value);
    }

    public static <T> RegistryKey<Registry<T>> ofRegistry(Identifier registry) {
        return of(Registry.ROOT_IDENTIFIER, registry);
    }

    public Identifier value() {
        return this.value;
    }

    public Identifier registry() {
        return this.registry;
    }

    @Override
    public String toString() {
        return this.registry.toString() + "/" + this.value.toString();
    }
}
