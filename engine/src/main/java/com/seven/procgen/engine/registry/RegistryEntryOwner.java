package com.seven.procgen.engine.registry;

public interface RegistryEntryOwner<T> {
    default boolean ownerEquals(RegistryEntryOwner<T> other) {
        return this.equals(other);
    }
}
