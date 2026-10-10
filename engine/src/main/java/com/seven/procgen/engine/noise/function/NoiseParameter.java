package com.seven.procgen.engine.noise.function;

import com.seven.procgen.engine.registry.Identifier;
import com.seven.procgen.engine.registry.Registries;
import com.seven.procgen.engine.registry.RegistryKey;

public record NoiseParameter<T>(T value) {
    public static final RegistryKey<NoiseParameter<Long>> SEED = create("seed");
    public static final RegistryKey<NoiseParameter<Double>> FREQUENCY = create("frequency");

    @SuppressWarnings("unchecked")
    private static <T> RegistryKey<NoiseParameter<T>> create(String id) {
        return (RegistryKey<NoiseParameter<T>>)(RegistryKey<?>)RegistryKey.of(Registries.NOISE_PARAMETER, Identifier.of(id));
    }
}
