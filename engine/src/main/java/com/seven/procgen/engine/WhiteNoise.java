package com.seven.procgen.engine;

public final class WhiteNoise implements Noise2D {
    private final long seed;

    public WhiteNoise(long seed) { this.seed = seed; }

    @Override
    public double sample(double x, double y) {
        long h = seed
            ^ ((long) Math.floor(x) * 0x9E3779B97F4A7C15L)
            ^ ((long) Math.floor(y) * 0xC2B2AE3D27D4EB4FL);
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        return (h >>> 11) * 0x1.0p-53;
    }
}
