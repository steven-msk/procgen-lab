package com.seven.procgen.engine.noise.combiner;

import com.mojang.serialization.Codec;
import com.seven.procgen.engine.noise.graph.ParameterDescriptor;
import com.seven.procgen.engine.noise.sampler.NoiseSamplerProvider;
import com.seven.procgen.engine.random.RandomSource;
import com.seven.procgen.engine.registry.Identifier;
import com.seven.procgen.engine.registry.Registries;
import com.seven.procgen.engine.registry.Registry;
import com.seven.procgen.engine.registry.RegistryKey;

public final class NoiseCombinerType<T extends NoiseCombiner.Parameters> implements NoiseCombinerProvider<T> {
    public static final RegistryKey<NoiseCombinerProvider<? extends NoiseCombiner.Parameters>> FBM = register("fbm");
    public static final RegistryKey<NoiseCombinerProvider<? extends NoiseCombiner.Parameters>> RIDGED_MULTIFRACTAL = register("ridged_multifractal");
    private final NoiseCombinerGetter<T> getter;
    private final Codec<T> parametersCodec;
    private final ParameterDescriptor<T> parameterDescriptor;
    private final T defaultParameters;

    public NoiseCombinerType(
        NoiseCombinerGetter<T> getter, Codec<T> parametersCodec, ParameterDescriptor<T> parameterDescriptor, T defaultParameters
    ) {
        this.getter = getter;
        this.parametersCodec = parametersCodec;
        this.parameterDescriptor = parameterDescriptor;
        this.defaultParameters = defaultParameters;
    }

    private static RegistryKey<NoiseCombinerProvider<? extends NoiseCombiner.Parameters>> register(String id) {
        return RegistryKey.of(Registries.NOISE_COMBINER, Identifier.of(id));
    }

    public static NoiseCombinerProvider<?> init(Registry<NoiseCombinerProvider<? extends NoiseCombiner.Parameters>> registry) {
        Registry.register(registry, RIDGED_MULTIFRACTAL, new NoiseCombinerType<>(
            RidgedMultifractalCombiner::new, RidgedMultifractalCombiner.Parameters.CODEC,
            RidgedMultifractalCombiner.Parameters.DESCRIPTOR, RidgedMultifractalCombiner.Parameters.DEFAULT
        ));
        return Registry.register(registry, FBM, new NoiseCombinerType<>(
            FBMCombiner::new, FBMCombiner.Parameters.CODEC, FBMCombiner.Parameters.DESCRIPTOR, FBMCombiner.Parameters.DEFAULT
        ));
    }

    @Override
    public NoiseCombiner<T> getCombiner(RandomSource random, NoiseSamplerProvider<?> samplerProvider, T parameters) {
        return getter.getCombiner(random, samplerProvider, parameters);
    }

    @Override
    public Codec<T> parametersCodec() {
        return parametersCodec;
    }

    @Override
    public ParameterDescriptor<T> parameterDescriptor() {
        return parameterDescriptor;
    }

    @Override
    public T defaultParameters() {
        return defaultParameters;
    }
}
