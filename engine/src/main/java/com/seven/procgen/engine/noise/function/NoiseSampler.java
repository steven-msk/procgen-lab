package com.seven.procgen.engine.noise.function;

import com.google.common.collect.ImmutableMap;
import com.seven.procgen.engine.registry.RegistryEntry;
import com.seven.procgen.engine.registry.RegistryKey;
import org.jspecify.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Supplier;

public interface NoiseSampler {
    /// Returns a value in [-1, 1]. Must be deterministic and thread-safe
    double getNoise(double x, double y);

    final class Parameters {
        private final ImmutableMap<RegistryKey<NoiseParameter<?>>, RegistryEntry<NoiseParameter<?>>> entries;

        public Parameters(ImmutableMap<RegistryKey<NoiseParameter<?>>, RegistryEntry<NoiseParameter<?>>> entries) {
            this.entries = entries;
        }

        public static NoiseSampler.Parameters.Builder builder() {
            return new Builder();
        }

        @SuppressWarnings("unchecked")
        public <T> @Nullable RegistryEntry<NoiseParameter<T>> getEntry(RegistryKey<NoiseParameter<T>> key) {
            RegistryEntry<NoiseParameter<?>> entry = this.entries.get(key);
            return entry != null ? (RegistryEntry<NoiseParameter<T>>)(RegistryEntry<?>)entry : null;
        }

        public <T> @Nullable NoiseParameter<T> get(RegistryKey<NoiseParameter<T>> key) {
            RegistryEntry<NoiseParameter<T>> entry = this.getEntry(key);
            return entry != null ? entry.value() : null;
        }

        public <T> Optional<NoiseParameter<T>> getOptional(RegistryKey<NoiseParameter<T>> key) {
            return Optional.ofNullable(this.get(key));
        }

        public <T> T getValue(RegistryKey<NoiseParameter<T>> key, T fallback) {
            return this.getValueOrElse(key, () -> fallback);
        }

        public <T> T getValueOrThrow(RegistryKey<NoiseParameter<T>> key) {
            return this.getOptional(key).map(NoiseParameter::value).orElseThrow(() -> new NoSuchElementException("Parameter not found: " + key.value()));
        }

        public <T> T getValueOrElse(RegistryKey<NoiseParameter<T>> key, Supplier<T> orElse) {
            return this.getOptional(key).map(NoiseParameter::value).orElseGet(orElse);
        }

        public static class Builder {
            private final ImmutableMap.Builder<RegistryKey<NoiseParameter<?>>, RegistryEntry<NoiseParameter<?>>> entries = ImmutableMap.builder();

            public <T> Builder add(RegistryKey<NoiseParameter<T>> key, T value) {
                return this.add(key, new NoiseParameter<>(value));
            }

            public <T> Builder add(RegistryKey<NoiseParameter<T>> key, NoiseParameter<T> value) {
                return this.add(key, RegistryEntry.of(key, value));
            }

            @SuppressWarnings("unchecked")
            private <T> Builder add(RegistryKey<NoiseParameter<T>> key, RegistryEntry<NoiseParameter<T>> entry) {
                this.entries.put((RegistryKey<NoiseParameter<?>>)(RegistryKey<?>)key, (RegistryEntry<NoiseParameter<?>>)(RegistryEntry<?>)entry);
                return this;
            }

            public Parameters build() {
                return new Parameters(this.entries.build());
            }
        }
    }
}
