package com.seven.procgen.engine.registry;

import com.seven.procgen.engine.noise.function.NoiseFunction;
import com.seven.procgen.engine.noise.function.NoiseParameter;

public final class Registries {
    public static final RegistryKey<Registry<NoiseParameter<?>>> NOISE_PARAMETER = create("noise_parameter");
    public static final RegistryKey<Registry<NoiseFunction>> NOISE_FUNCTION = create("noise_function");

    private static <T> RegistryKey<Registry<T>> create(String id) {
        return RegistryKey.ofRegistry(Identifier.of(id));
    }
}
