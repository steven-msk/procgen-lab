package com.seven.procgen.engine.noise.sampler;

import com.mojang.serialization.Codec;
import com.seven.procgen.engine.registry.Registries;
import com.seven.procgen.engine.registry.Registry;
import com.seven.procgen.engine.registry.RegistryKey;

@FunctionalInterface
public interface NoiseSamplerProvider {
    Codec<NoiseSamplerProvider> CODEC = Registry.NOISE_SAMPLER_PROVIDER.byNameCodec();
    Codec<RegistryKey<NoiseSamplerProvider>> KEY_CODEC = RegistryKey.codec(Registries.NOISE_SAMPLER_PROVIDER);

    NoiseSampler getSampler(long seed);
}
