package com.seven.procgen.engine.noise.combiner;

import com.seven.procgen.engine.noise.sampler.NoiseSamplerProvider;
import com.seven.procgen.engine.random.RandomSource;

public abstract class NoiseCombiner<T extends NoiseCombiner.Parameters> {
    protected final RandomSource random;
    protected final NoiseSamplerProvider samplerProvider;
    protected final Parameters parameters;

    protected NoiseCombiner(RandomSource random, NoiseSamplerProvider samplerProvider, Parameters parameters) {
        this.random = random;
        this.samplerProvider = samplerProvider;
        this.parameters = parameters;
    }

    public abstract double compute(double x, double y);

    public interface Parameters {
    }
}
