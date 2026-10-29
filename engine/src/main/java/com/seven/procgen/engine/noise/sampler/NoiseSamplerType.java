package com.seven.procgen.engine.noise.sampler;

import com.seven.procgen.engine.registry.Registry;

public final class NoiseSamplerType implements NoiseSamplerProvider {
    public static final NoiseSamplerType WHITE = register("white", WhiteNoise::new);
    public static final NoiseSamplerType PERLIN = register("perlin", PerlinNoise::new);
    private final NoiseSamplerProvider provider;

    private static NoiseSamplerType register(String id, NoiseSamplerProvider provider) {
        return Registry.register(Registry.NOISE_SAMPLER_PROVIDER, id, new NoiseSamplerType(provider));
    }

    public static NoiseSamplerProvider init() {
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
