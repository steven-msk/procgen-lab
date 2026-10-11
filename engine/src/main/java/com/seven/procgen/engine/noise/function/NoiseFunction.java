package com.seven.procgen.engine.noise.function;

public interface NoiseFunction {
    NoiseSampler createSampler(long seed);
}
