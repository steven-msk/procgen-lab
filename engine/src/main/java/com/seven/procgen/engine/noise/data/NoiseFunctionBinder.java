package com.seven.procgen.engine.noise.data;

import com.seven.procgen.engine.noise.function.NoiseFunction;

/// Turns the unbound tree into one that can be sampled.
/// Essentially this attaches sampler and combiner instances based on a seed fork.
@FunctionalInterface
public interface NoiseFunctionBinder {
    NoiseFunction bind(NoiseFunction unbound);
}
