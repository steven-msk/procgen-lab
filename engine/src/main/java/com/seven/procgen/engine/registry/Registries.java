package com.seven.procgen.engine.registry;

import com.mojang.serialization.MapCodec;
import com.seven.procgen.engine.noise.combiner.NoiseCombiner;
import com.seven.procgen.engine.noise.combiner.NoiseCombinerProvider;
import com.seven.procgen.engine.noise.function.NoiseFunction;
import com.seven.procgen.engine.noise.sampler.NoiseSamplerProvider;

public final class Registries {
    public static final RegistryKey<Registry<NoiseSamplerProvider<?>>> NOISE_SAMPLER_PROVIDER = create("noise_sampler");
    public static final RegistryKey<Registry<NoiseCombinerProvider<? extends NoiseCombiner.Parameters>>> NOISE_COMBINER_PROVIDER = create("noise_combiner");
    public static final RegistryKey<Registry<MapCodec<? extends NoiseFunction>>> NOISE_FUNCTION = create("noise_function");

    private static <T> RegistryKey<Registry<T>> create(String id) {
        return RegistryKey.ofRegistry(Identifier.of(id));
    }
}
