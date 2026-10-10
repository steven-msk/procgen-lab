package com.seven.procgen.engine.registry;

import java.util.Objects;

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
        return "Key[" + this.registry.toString() + " : " + this.value.toString() + "]";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof RegistryKey<?> other
                && other.registry.equals(this.registry)
                && other.value.equals(this.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.registry, this.value);
    }
}
