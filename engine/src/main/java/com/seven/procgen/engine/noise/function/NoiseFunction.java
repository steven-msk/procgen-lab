package com.seven.procgen.engine.noise.function;

public interface NoiseFunction {
    NoiseSampler createSampler(NoiseSampler.Parameters parameters);
}
