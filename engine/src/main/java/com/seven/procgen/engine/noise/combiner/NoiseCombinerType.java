package com.seven.procgen.engine.noise.combiner;

import com.mojang.serialization.Codec;
import com.seven.procgen.engine.noise.sampler.NoiseSamplerProvider;
import com.seven.procgen.engine.random.RandomSource;
import com.seven.procgen.engine.registry.Registry;

public final class NoiseCombinerType<T extends NoiseCombiner.Parameters> implements NoiseCombinerProvider<T> {
    public static final NoiseCombinerType<FBMCombiner.Parameters> FBM = register("fbm", FBMCombiner::new, FBMCombiner.Parameters.CODEC);
    public static final NoiseCombinerType<RidgedMultifractalCombiner.Parameters> RIDGED_MULTIFRACTAL = register("ridged_multifractal", RidgedMultifractalCombiner::new, RidgedMultifractalCombiner.Parameters.CODEC);
    private final NoiseCombinerGetter<T> getter;
    private final Codec<T> parametersCodec;

    public NoiseCombinerType(NoiseCombinerGetter<T> getter, Codec<T> parametersCodec) {
        this.getter = getter;
        this.parametersCodec = parametersCodec;
    }

    private static <T extends NoiseCombiner.Parameters> NoiseCombinerType<T> register(String id, NoiseCombinerGetter<T> getter, Codec<T> parametersCodec) {
        return Registry.register(Registry.NOISE_COMBINER_PROVIDER, id, new NoiseCombinerType<>(getter, parametersCodec));
    }

    public static NoiseCombinerProvider<?> init() {
        return FBM;
    }

    @Override
    public NoiseCombiner<T> getCombiner(RandomSource random, NoiseSamplerProvider samplerProvider, T parameters) {
        return getter.getCombiner(random, samplerProvider, parameters);
    }

    @Override
    public Codec<T> parametersCodec() {
        return parametersCodec;
    }
}
