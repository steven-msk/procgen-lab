package com.seven.procgen.engine.noise.sampler;

public final class PerlinNoise implements NoiseSampler {
    // 12 cube-edge gradients, padded to 16 so the hash can be masked with & 15.
    // All have length sqrt(2), which bounds the output to [-1, 1], so no rescale is needed.
    private static final double[] GRAD_X = {1, -1, 1, -1, 1, -1, 1, -1, 0, 0, 0, 0, 1, -1, 0, 0};
    private static final double[] GRAD_Y = {1, 1, -1, -1, 0, 0, 0, 0, 1, -1, 1, -1, 1, 1, -1, -1};
    private static final double[] GRAD_Z = {0, 0, 0, 0, 1, 1, -1, -1, 1, 1, -1, -1, 0, 0, 1, -1};
    private final int[] perm;

    public PerlinNoise(long seed) {
        this.perm = buildPermutation(seed);
    }

    @Override
    public double sample(double x, double y, double z) {
        long x0 = (long)Math.floor(x);
        long y0 = (long)Math.floor(y);
        long z0 = (long)Math.floor(z);
        double fx = x - x0;
        double fy = y - y0;
        double fz = z - z0;

        int xi = (int)(x0 & 255);
        int yi = (int)(y0 & 255);
        int zi = (int)(z0 & 255);

        // Hash the 8 cube corners (max index is 511, so the doubled table never needs wrapping)
        int a = perm[xi] + yi;
        int b = perm[xi + 1] + yi;
        int aa = perm[a] + zi;
        int ab = perm[a + 1] + zi;
        int ba = perm[b] + zi;
        int bb = perm[b + 1] + zi;

        double n000 = grad(perm[aa], fx, fy, fz);
        double n100 = grad(perm[ba], fx - 1.0, fy, fz);
        double n010 = grad(perm[ab], fx, fy - 1.0, fz);
        double n110 = grad(perm[bb], fx - 1.0, fy - 1.0, fz);
        double n001 = grad(perm[aa + 1], fx, fy, fz - 1.0);
        double n101 = grad(perm[ba + 1], fx - 1.0, fy, fz - 1.0);
        double n011 = grad(perm[ab + 1], fx, fy - 1.0, fz - 1.0);
        double n111 = grad(perm[bb + 1], fx - 1.0, fy - 1.0, fz - 1.0);

        double u = fade(fx);
        double v = fade(fy);
        double w = fade(fz);

        double nx00 = lerp(u, n000, n100);
        double nx10 = lerp(u, n010, n110);
        double nx01 = lerp(u, n001, n101);
        double nx11 = lerp(u, n011, n111);

        double nxy0 = lerp(v, nx00, nx10);
        double nxy1 = lerp(v, nx01, nx11);

        return lerp(w, nxy0, nxy1);
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

    private static double grad(int hash, double dx, double dy, double dz) {
        int g = hash & 15;
        return GRAD_X[g] * dx + GRAD_Y[g] * dy + GRAD_Z[g] * dz;
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6.0d - 15.0d) + 10.0d);
    }
}