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

    public NoiseCombinerType(NoiseCombinerGetter<T> getter, Codec<T> parametersCodec, ParameterDescriptor<T> parameterDescriptor) {
        this.getter = getter;
        this.parametersCodec = parametersCodec;
        this.parameterDescriptor = parameterDescriptor;
    }

    private static RegistryKey<NoiseCombinerProvider<? extends NoiseCombiner.Parameters>> register(String id) {
        return RegistryKey.of(Registries.NOISE_COMBINER_PROVIDER, Identifier.of(id));
    }

    public static NoiseCombinerProvider<?> init(Registry<NoiseCombinerProvider<? extends NoiseCombiner.Parameters>> registry) {
        Registry.register(registry, RIDGED_MULTIFRACTAL, new NoiseCombinerType<>(
            RidgedMultifractalCombiner::new, RidgedMultifractalCombiner.Parameters.CODEC, RidgedMultifractalCombiner.Parameters.DESCRIPTOR
        ));
        return Registry.register(registry, FBM, new NoiseCombinerType<>(
            FBMCombiner::new, FBMCombiner.Parameters.CODEC, FBMCombiner.Parameters.DESCRIPTOR
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
}
