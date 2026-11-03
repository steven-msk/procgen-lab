package com.seven.procgen.engine.noise.sampler;

import com.mojang.serialization.Codec;
import com.seven.procgen.engine.noise.graph.ParameterDescriptor;
import com.seven.procgen.engine.registry.Registries;
import com.seven.procgen.engine.registry.Registry;
import com.seven.procgen.engine.registry.RegistryKey;

public interface NoiseSamplerProvider<T> {
    Codec<NoiseSamplerProvider<?>> CODEC = Registry.NOISE_SAMPLER_PROVIDER.byNameCodec();
    Codec<RegistryKey<NoiseSamplerProvider<?>>> KEY_CODEC = RegistryKey.codec(Registries.NOISE_SAMPLER_PROVIDER);

    NoiseSampler getSampler(long seed, T parameters);

    Codec<T> parametersCodec();

    T defaultParameters();

    ParameterDescriptor<T> parameterDescriptor();

    default NoiseSampler getSampler(long seed) {
        return getSampler(seed, defaultParameters());
    }
}
