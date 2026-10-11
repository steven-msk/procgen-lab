package com.seven.procgen.engine.noise.sampler;

public interface NoiseSampler {
    /// Returns a value in [-1, 1]. Must be deterministic and thread-safe
    double sample(double x, double y);
}
