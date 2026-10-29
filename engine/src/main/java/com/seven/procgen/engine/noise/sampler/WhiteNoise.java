package com.seven.procgen.engine.noise.sampler;

/// Basic white noise. Returns in [-1, 1]
public class WhiteNoise implements NoiseSampler {
    private final long seed;

    public WhiteNoise(long seed) {
        this.seed = seed;
    }

    @Override
    public double sample(double x, double y, double z) {
        long h = seed
                ^ ((long)Math.floor(x) * 0x9E3779B97F4A7C15L)
                ^ ((long)Math.floor(y) * 0xC2B2AE3D27D4EB4FL)
                ^ ((long)Math.floor(z) * 0x165667B19E3779F9L);
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        return ((h >>> 11) * 0x1.0p-53) * 2.0d - 1.0d;
    }
}