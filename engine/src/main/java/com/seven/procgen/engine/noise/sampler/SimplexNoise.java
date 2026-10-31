package com.seven.procgen.engine.noise.sampler;

import com.seven.procgen.engine.util.math.NoiseMath;

/// 3D simplex noise (Gustavson), hash-based gradients instead of a permutation table so it works with any seed.
/// Output is approximately [-1, 1]. For 2D use, sample at a fixed z.
public final class SimplexNoise implements NoiseSampler {
    private static final double F3 = 1.0d / 3.0d;
    private static final double G3 = 1.0d / 6.0d;
    // the 12 cube-edge midpoints
    private static final int[] GRAD = {
        1, 1, 0, -1, 1, 0, 1, -1, 0, -1, -1, 0,
        1, 0, 1, -1, 0, 1, 1, 0, -1, -1, 0, -1,
        0, 1, 1, 0, -1, 1, 0, 1, -1, 0, -1, -1
    };

    private final long seed;

    public SimplexNoise(long seed) {
        this.seed = seed;
    }

    public double sample(double x, double y) {
        return sample(x, y, 0.0d);
    }

    @Override
    public double sample(double x, double y, double z) {
        double s = (x + y + z) * F3;
        long i = NoiseMath.floor(x + s);
        long j = NoiseMath.floor(y + s);
        long k = NoiseMath.floor(z + s);
        double t = (i + j + k) * G3;
        double x0 = x - (i - t);
        double y0 = y - (j - t);
        double z0 = z - (k - t);

        int i1, j1, k1, i2, j2, k2; // offsets of the 2nd and 3rd simplex corners
        if (x0 >= y0) {
            if (y0 >= z0) {
                i1 = 1;
                j1 = 0;
                k1 = 0;
                i2 = 1;
                j2 = 1;
                k2 = 0;
            } else if (x0 >= z0) {
                i1 = 1;
                j1 = 0;
                k1 = 0;
                i2 = 1;
                j2 = 0;
                k2 = 1;
            } else {
                i1 = 0;
                j1 = 0;
                k1 = 1;
                i2 = 1;
                j2 = 0;
                k2 = 1;
            }
        } else {
            if (y0 < z0) {
                i1 = 0;
                j1 = 0;
                k1 = 1;
                i2 = 0;
                j2 = 1;
                k2 = 1;
            } else if (x0 < z0) {
                i1 = 0;
                j1 = 1;
                k1 = 0;
                i2 = 0;
                j2 = 1;
                k2 = 1;
            } else {
                i1 = 0;
                j1 = 1;
                k1 = 0;
                i2 = 1;
                j2 = 1;
                k2 = 0;
            }
        }

        double sum = corner(i, j, k, x0, y0, z0)
            + corner(i + i1, j + j1, k + k1, x0 - i1 + G3, y0 - j1 + G3, z0 - k1 + G3)
            + corner(i + i2, j + j2, k + k2, x0 - i2 + 2.0d * G3, y0 - j2 + 2.0d * G3, z0 - k2 + 2.0d * G3)
            + corner(i + 1, j + 1, k + 1, x0 - 1.0d + 3.0d * G3, y0 - 1.0d + 3.0d * G3, z0 - 1.0d + 3.0d * G3);
        return 32.0d * sum;
    }

    private double corner(long i, long j, long k, double x, double y, double z) {
        double t = 0.6d - x * x - y * y - z * z;
        if (t < 0.0d) return 0.0d;
        t *= t;
        int g = (int)((NoiseMath.hash(seed, i, j, k) >>> 33) % 12) * 3;
        return t * t * (GRAD[g] * x + GRAD[g + 1] * y + GRAD[g + 2] * z);
    }
}

