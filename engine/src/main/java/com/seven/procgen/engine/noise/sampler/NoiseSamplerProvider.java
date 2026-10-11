package com.seven.procgen.engine.noise.sampler;

public interface NoiseSamplerProvider {
    NoiseSampler getSampler(long seed);
}
