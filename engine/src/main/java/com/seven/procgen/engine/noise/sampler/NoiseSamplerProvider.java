package com.seven.procgen.engine.noise.sampler;

import com.mojang.serialization.Codec;
import com.seven.procgen.engine.registry.Registry;

@FunctionalInterface
public interface NoiseSamplerProvider {
    Codec<NoiseSamplerProvider> CODEC = Registry.NOISE_SAMPLER_PROVIDER.byNameCodec();

    NoiseSampler getSampler(long seed);
}
