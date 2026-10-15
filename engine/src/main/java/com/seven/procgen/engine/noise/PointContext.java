package com.seven.procgen.engine.noise;

import com.seven.procgen.engine.noise.function.NoiseFunction;

public class PointContext implements NoiseFunction.Context {
    private double x;
    private double y;

    public PointContext(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public PointContext() {
        this(0, 0);
    }

    public PointContext set(double x, double y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public PointContext setX(double x) {
        this.x = x;
        return this;
    }

    public PointContext setY(double y) {
        this.y = y;
        return this;
    }

    @Override
    public double x() {
        return x;
    }

    @Override
    public double y() {
        return y;
    }
}
