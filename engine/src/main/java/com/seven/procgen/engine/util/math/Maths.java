package com.seven.procgen.engine.util.math;

public final class Maths {
    private Maths() {
    }

    public static double clamp01(double v) {
        return v < 0.0d ? 0.0d : Math.min(v, 1.0d);
    }

    public static double smoothstep(double t) {
        return t * t * (3.0d - 2.0d * t);
    }
}
