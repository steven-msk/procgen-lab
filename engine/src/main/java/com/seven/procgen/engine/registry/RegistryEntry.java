package com.seven.procgen.engine.registry;

import java.util.Objects;
import java.util.function.Predicate;

public final class RegistryEntry<T> {
    private final RegistryEntryOwner<T> owner;
    private final RegistryKey<T> registryKey;
    private final T value;

    private RegistryEntry(RegistryEntryOwner<T> owner, RegistryKey<T> registryKey, T value) {
        this.owner = owner;
        this.registryKey = registryKey;
        this.value = value;
    }

    public static <T> RegistryEntry<T> of(RegistryEntryOwner<T> owner, RegistryKey<T> key, T value) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(key, "key");
        return new RegistryEntry<>(owner, key, value);
    }

    public boolean matches(Predicate<RegistryKey<T>> predicate) {
        return predicate.test(this.registryKey);
    }

    public boolean matchesId(Identifier id) {
        return this.registryKey.value().equals(id);
    }

    public boolean matchesKey(RegistryKey<T> key) {
        return this.registryKey.equals(key);
    }

    public RegistryKey<T> key() {
        return this.registryKey;
    }

    public T value() {
        return this.value;
    }

    public boolean ownerEquals(RegistryEntryOwner<T> other) {
        return this.owner.ownerEquals(other);
    }

    @Override
    public String toString() {
        return "Entry[" + this.registryKey + "=" + this.value + "]";
    }

    public String getIdAsString() {
        return this.registryKey.value().toString();
    }
}
