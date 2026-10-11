package com.seven.procgen.engine.noise.sampler;

public final class PerlinNoise implements NoiseSampler {
    private static final double INV_SQRT2 = 0.7071067811865476d;
    private static final double SCALE = 1.4142135623730951d; // sqrt(2): stretches [-0.707, 0.707] to [-1, 1]
    private static final double[] GRAD_X = {1, -1, 0, 0, INV_SQRT2, -INV_SQRT2, INV_SQRT2, -INV_SQRT2};
    private static final double[] GRAD_Y = {0, 0, 1, -1, INV_SQRT2, INV_SQRT2, -INV_SQRT2, -INV_SQRT2};
    private final int[] perm;

    public PerlinNoise(long seed) {
        this.perm = buildPermutation(seed);
    }

    @Override
    public double sample(double x, double y) {
        long x0 = (long)Math.floor(x);
        long y0 = (long)Math.floor(y);
        double fx = x - x0;
        double fy = y - y0;

        int xi = (int)(x0 & 255);
        int yi = (int)(y0 & 255);

        double n00 = grad(perm[perm[xi] + yi], fx, fy);
        double n10 = grad(perm[perm[xi + 1] + yi], fx - 1.0d, fy);
        double n01 = grad(perm[perm[xi] + yi + 1], fx, fy - 1.0d);
        double n11 = grad(perm[perm[xi + 1] + yi + 1], fx - 1.0d, fy - 1.0d);

        double u = fade(fx);
        double v = fade(fy);

        double nx0 = n00 + u * (n10 - n00);
        double nx1 = n01 + u * (n11 - n01);
        return (nx0 + v * (nx1 - nx0)) * SCALE;
    }

    private static int[] buildPermutation(long seed) {
        int[] p = new int[256];
        for (int i = 0; i < 256; i++) p[i] = i;

        long state = seed;
        for (int i = 255; i > 0; i--) { // Fisher-Yates
            state = splitMix64(state);
            int j = (int)((state >>> 33) % (i + 1));
            int tmp = p[i];
            p[i] = p[j];
            p[j] = tmp;
        }

        int[] perm = new int[512]; // doubled so the +1 lookups never need wrapping
        for (int i = 0; i < 512; i++) perm[i] = p[i & 255];
        return perm;
    }

    private static long splitMix64(long z) {
        z += 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static double grad(int hash, double dx, double dy) {
        int g = hash & 7;
        return GRAD_X[g] * dx + GRAD_Y[g] * dy;
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6.0d - 15.0d) + 10.0d);
    }
}
