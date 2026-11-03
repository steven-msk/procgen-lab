package com.seven.procgen.engine.noise.sampler;

import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import com.seven.procgen.engine.noise.graph.ParameterDescriptor;
import com.seven.procgen.engine.registry.Identifier;
import com.seven.procgen.engine.registry.Registries;
import com.seven.procgen.engine.registry.Registry;
import com.seven.procgen.engine.registry.RegistryKey;

import java.util.function.BiFunction;
import java.util.function.LongFunction;

public final class NoiseSamplerType<T> implements NoiseSamplerProvider<T> {
    public static final RegistryKey<NoiseSamplerProvider<?>> WHITE = register("white");
    public static final RegistryKey<NoiseSamplerProvider<?>> PERLIN = register("perlin");
    public static final RegistryKey<NoiseSamplerProvider<?>> VALUE = register("value");
    public static final RegistryKey<NoiseSamplerProvider<?>> SIMPLEX = register("simplex");
    public static final RegistryKey<NoiseSamplerProvider<?>> WORLEY = register("worley");
    private final BiFunction<Long, T, NoiseSampler> samplerFunction;
    private final Codec<T> parametersCodec;
    private final T defaultParameters;
    private final ParameterDescriptor<T> parameterDescriptor;

    private static RegistryKey<NoiseSamplerProvider<?>> register(String id) {
        return RegistryKey.of(Registries.NOISE_SAMPLER, Identifier.of(id));
    }

    public static NoiseSamplerProvider<?> init(Registry<NoiseSamplerProvider<?>> registry) {
        Registry.register(registry, PERLIN, parameterless(PerlinNoise::new));
        Registry.register(registry, VALUE, parameterless(ValueNoise::new));
        Registry.register(registry, SIMPLEX, parameterless(SimplexNoise::new));
        Registry.register(registry, WORLEY, parametrized(WorleyNoise::new,
            WorleyNoise.Parameters.CODEC, WorleyNoise.Parameters.DEFAULT,
            WorleyNoise.Parameters.DESCRIPTOR
        ));
        return Registry.register(registry, WHITE, parameterless(WhiteNoise::new));
    }

    private static NoiseSamplerType<Unit> parameterless(LongFunction<NoiseSampler> factory) {
        return new NoiseSamplerType<>((seed, u) -> factory.apply(seed), Codec.unit(Unit.INSTANCE), Unit.INSTANCE, ParameterDescriptor.none(Unit.INSTANCE));
    }

    private static <T> NoiseSamplerType<T> parametrized(
        BiFunction<Long, T, NoiseSampler> factory, Codec<T> parametersCodec, T defaultParameters, ParameterDescriptor<T> parameterDescriptor
    ) {
        return new NoiseSamplerType<>(factory, parametersCodec, defaultParameters, parameterDescriptor);
    }

    private NoiseSamplerType(
        BiFunction<Long, T, NoiseSampler> samplerFunction, Codec<T> parametersCodec, T defaultParameters, ParameterDescriptor<T> parameterDescriptor
    ) {
        this.samplerFunction = samplerFunction;
        this.parametersCodec = parametersCodec;
        this.defaultParameters = defaultParameters;
        this.parameterDescriptor = parameterDescriptor;
    }

    @Override
    public NoiseSampler getSampler(long seed, T parameters) {
        return samplerFunction.apply(seed, parameters);
    }

    @Override
    public Codec<T> parametersCodec() {
        return parametersCodec;
    }

    @Override
    public T defaultParameters() {
        return defaultParameters;
    }

    @Override
    public ParameterDescriptor<T> parameterDescriptor() {
        return parameterDescriptor;
    }
}
