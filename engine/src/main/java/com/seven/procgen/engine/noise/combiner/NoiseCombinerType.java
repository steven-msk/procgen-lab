package com.seven.procgen.engine.noise.combiner;

import com.seven.procgen.engine.noise.sampler.NoiseSamplerProvider;
import com.seven.procgen.engine.random.RandomSource;
import com.seven.procgen.engine.registry.Registry;
import com.seven.procgen.engine.registry.RegistryEntryLookup;

public final class NoiseCombinerType<T extends NoiseCombiner.Parameters> implements NoiseCombinerProvider<T> {
    public static final NoiseCombinerType<FBMCombiner.Parameters> FBM = register("fbm", FBMCombiner::new);
    public static final NoiseCombinerType<RidgedMultifractalCombiner.Parameters> RIDGE_MULTIFRACTAL = register("ridge_multifractal", RidgedMultifractalCombiner::new);
    private final NoiseCombinerProvider<T> provider;

    private static <T extends NoiseCombiner.Parameters> NoiseCombinerType<T> register(String id, NoiseCombinerProvider<T> provider) {
        return Registry.register(Registry.NOISE_COMBINER_TYPE, id, new NoiseCombinerType<>(provider));
    }

    public static NoiseCombinerType<?> init(Registry<NoiseCombinerType<?>> registry, RegistryEntryLookup.RegistryLookup lookup) {
        return FBM;
    }

    public NoiseCombinerType(NoiseCombinerProvider<T> provider) {
        this.provider = provider;
    }

    @Override
    public NoiseCombiner<T> getCombiner(RandomSource random, NoiseSamplerProvider samplerProvider, T parameters) {
        return provider.getCombiner(random, samplerProvider, parameters);
    }
}
