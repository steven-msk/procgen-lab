package com.seven.procgen.engine.noise.sampler;

import com.seven.procgen.engine.util.math.NoiseMath;

/// Value noise: random value per lattice point, smoothly interpolated (quintic fade). Output in [-1, 1].
public final class ValueNoise implements NoiseSampler {
    private final long seed;

    public ValueNoise(long seed) {
        this.seed = seed;
    }

    @Override
    public double sample(double x, double y, double z) {
        long xi = NoiseMath.floor(x);
        long yi = NoiseMath.floor(y);
        long zi = NoiseMath.floor(z);
        double u = NoiseMath.fade(x - xi);
        double v = NoiseMath.fade(y - yi);
        double fz = z - zi;

        double y0 = layer(xi, yi, zi, u, v);
        if (fz == 0.0d) return y0; // integer z (the common 2D case): skip the second layer
        double y1 = layer(xi, yi, zi + 1, u, v);
        return NoiseMath.lerp(y0, y1, NoiseMath.fade(fz));
    }

    private double layer(long xi, long yi, long zi, double u, double v) {
        double c00 = NoiseMath.signed(NoiseMath.hash(seed, xi, yi, zi));
        double c10 = NoiseMath.signed(NoiseMath.hash(seed, xi + 1, yi, zi));
        double c01 = NoiseMath.signed(NoiseMath.hash(seed, xi, yi + 1, zi));
        double c11 = NoiseMath.signed(NoiseMath.hash(seed, xi + 1, yi + 1, zi));
        return NoiseMath.lerp(NoiseMath.lerp(c00, c10, u), NoiseMath.lerp(c01, c11, u), v);
    }

}
