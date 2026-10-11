package com.seven.procgen.engine.util.math;

public final class NoiseMath {
    public static final double WRAP_FACTOR = 3.3554432E7d;

    public static double wrap(double value) {
        return value - (long)Math.floor(value / WRAP_FACTOR + 0.5d) * WRAP_FACTOR;
    }
}
