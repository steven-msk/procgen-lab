package com.seven.procgen.engine.noise.data;

import com.google.common.collect.Maps;
import com.seven.procgen.engine.noise.combiner.NoiseCombiner;
import com.seven.procgen.engine.noise.function.NoiseFunction;
import com.seven.procgen.engine.random.JavaRandomSource;
import com.seven.procgen.engine.random.RandomFork;
import com.seven.procgen.engine.registry.RegistryEntryLookup;

import java.util.Map;

public class NoiseState implements NoiseFunction.Visitor {
    private final Map<NoiseFunction, NoiseFunction> functions = Maps.newIdentityHashMap();
    private final long seed;
    private final RandomFork randomFork;
    private final RegistryEntryLookup.RegistryLookup registryLookup;

    public NoiseState(long seed, RegistryEntryLookup.RegistryLookup registryLookup) {
        this.seed = seed;
        this.randomFork = new JavaRandomSource(seed).fork();
        this.registryLookup = registryLookup;
    }

    @Override
    public NoiseFunction apply(NoiseFunction input) {
        return functions.computeIfAbsent(input, this::wrapFunction);
    }

    @Override
    public <T> NoiseFunction.SamplerEntry<T> visitSampler(NoiseFunction.SamplerEntry<T> sampler) {
        return sampler.bind(randomFork, registryLookup);
    }

    @Override
    public <T extends NoiseCombiner.Parameters> NoiseFunction.CombinerEntry<T> visitCombiner(NoiseFunction.CombinerEntry<T> combiner) {
        return combiner.bind(randomFork, registryLookup);
    }

    private NoiseFunction wrapFunction(NoiseFunction function) {
        return function;
    }

    public long seed() {
        return seed;
    }
}
