package com.seven.procgen.engine.registry;

import com.seven.procgen.engine.noise.combiner.NoiseCombinerType;
import com.seven.procgen.engine.noise.sampler.NoiseSamplerType;

public final class Registries {
    public static final RegistryKey<Registry<NoiseSamplerType>> NOISE_SAMPLER_TYPE = create("noise_sampler");
    public static final RegistryKey<Registry<NoiseCombinerType<?>>> NOISE_COMBINER_TYPE = create("noise_combiner");

    private static <T> RegistryKey<Registry<T>> create(String id) {
        return RegistryKey.ofRegistry(Identifier.of(id));
    }
}
