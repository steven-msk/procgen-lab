package com.seven.procgen.engine.noise.sampler;

import com.seven.procgen.engine.registry.Identifier;
import com.seven.procgen.engine.registry.Registries;
import com.seven.procgen.engine.registry.Registry;
import com.seven.procgen.engine.registry.RegistryKey;

public final class NoiseSamplerType {
    public static final RegistryKey<NoiseSamplerProvider> WHITE = register("white");
    public static final RegistryKey<NoiseSamplerProvider> PERLIN = register("perlin");

    private static RegistryKey<NoiseSamplerProvider> register(String id) {
        return RegistryKey.of(Registries.NOISE_SAMPLER_PROVIDER, Identifier.of(id));
    }

    public static NoiseSamplerProvider init(Registry<NoiseSamplerProvider> registry) {
        Registry.register(registry, WHITE, WhiteNoise::new);
        return Registry.register(registry, PERLIN, PerlinNoise::new);
    }

    private NoiseSamplerType() {
    }
}
