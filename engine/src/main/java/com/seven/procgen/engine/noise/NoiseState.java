package com.seven.procgen.engine.noise;

import com.seven.procgen.engine.noise.combiner.NoiseCombiner;
import com.seven.procgen.engine.noise.function.NoiseFunction;
import com.seven.procgen.engine.random.JavaRandomSource;
import com.seven.procgen.engine.random.RandomFork;

public class NoiseState implements NoiseFunction.Visitor {
    private final long seed;
    private final RandomFork randomFork;

    public NoiseState(long seed) {
        this.seed = seed;
        this.randomFork = new JavaRandomSource(seed).fork();
    }

    @Override
    public NoiseFunction apply(NoiseFunction input) {
        return input;
    }

    @Override
    public NoiseFunction.SamplerEntry visitSampler(NoiseFunction.SamplerEntry sampler) {
        return sampler.bind(randomFork);
    }

    @Override
    public <T extends NoiseCombiner.Parameters> NoiseFunction.CombinerEntry<T> visitCombiner(NoiseFunction.CombinerEntry<T> combiner) {
        return combiner.bind(randomFork);
    }

    public long seed() {
        return seed;
    }
}
