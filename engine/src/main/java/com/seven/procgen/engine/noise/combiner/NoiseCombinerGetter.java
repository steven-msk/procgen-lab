package com.seven.procgen.engine.noise.combiner;

import com.seven.procgen.engine.noise.sampler.NoiseSamplerProvider;
import com.seven.procgen.engine.random.RandomSource;

public interface NoiseCombinerGetter<T extends NoiseCombiner.Parameters> {
    NoiseCombiner<T> getCombiner(RandomSource random, NoiseSamplerProvider samplerProvider, T parameters);
}
