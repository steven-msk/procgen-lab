package com.seven.procgen.engine.noise.sampler;

@FunctionalInterface
public interface NoiseSamplerProvider {
    NoiseSampler getSampler(long seed);
}
