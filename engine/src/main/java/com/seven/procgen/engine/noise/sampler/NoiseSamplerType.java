package com.seven.procgen.engine.noise.sampler;

import com.seven.procgen.engine.registry.Registry;
import com.seven.procgen.engine.registry.RegistryEntryLookup;

public final class NoiseSamplerType implements NoiseSamplerProvider {
    public static final NoiseSamplerType WHITE = register("white", WhiteNoise::new);
    public static final NoiseSamplerType PERLIN = register("perlin", PerlinNoise::new);
    private final NoiseSamplerProvider provider;

    private static NoiseSamplerType register(String id, NoiseSamplerProvider provider) {
        return Registry.register(Registry.NOISE_SAMPLER_TYPE, id, new NoiseSamplerType(provider));
    }

    public static NoiseSamplerType init(Registry<NoiseSamplerType> registry, RegistryEntryLookup.RegistryLookup lookup) {
        return WHITE;
    }

    public NoiseSamplerType(NoiseSamplerProvider provider) {
        this.provider = provider;
    }

    @Override
    public NoiseSampler getSampler(long seed) {
        return provider.getSampler(seed);
    }
}
