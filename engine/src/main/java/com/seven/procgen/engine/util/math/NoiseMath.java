package com.seven.procgen.engine.util.math;

public final class NoiseMath {
    private static final long PX = 0x9E3779B97F4A7C15L;
    private static final long PY = 0xC2B2AE3D27D4EB4FL;
    private static final long PZ = 0x165667B19E3779F9L;
    public static final double WRAP_FACTOR = 3.3554432E7d;

    private NoiseMath() {
    }

    public static double wrap(double value) {
        return value - (long)Math.floor(value / WRAP_FACTOR + 0.5d) * WRAP_FACTOR;
    }

    public static long hash(long seed, long x, long y, long z) {
        return mix(seed ^ (x * PX) ^ (y * PY) ^ (z * PZ));
    }

    public static long mix(long h) {
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return h;
    }

    /// [0, 1)
    public static double unit(long h) {
        return (h >>> 11) * 0x1.0p-53;
    }

    /// [-1, 1)
    public static double signed(long h) {
        return unit(h) * 2.0d - 1.0d;
    }

    public static double fade(double t) {
        return t * t * t * (t * (t * 6.0d - 15.0d) + 10.0d);
    }

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public static long floor(double v) {
        long i = (long)v;
        return v < i ? i - 1 : i;
    }
}
