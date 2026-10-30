package com.seven.procgen.engine.noise.data;

import com.mojang.serialization.MapCodec;
import com.seven.procgen.engine.noise.function.NoiseFunction;

import java.util.function.UnaryOperator;

public final class GridContext implements NoiseFunction.Context {
    private final double z;
    private int index;
    private double x;
    private double y;

    public GridContext(double z) {
        this.z = z;
    }

    public GridContext set(int index, double x, double y) {
        this.index = index;
        this.x = x;
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

    @Override
    public double z() {
        return z;
    }

    /// Transient wrapper that records what its wrapped node produced at each grid pixel
    /// Lives for exactly one sampling pass; never serialized or mapped.
    public static final class Probe implements NoiseFunction {
        private final NoiseFunction wrapped;
        private final double[] values;
        private int filled;
        private int lastIndex = -1;
        private double lastValue;

        public Probe(NoiseFunction wrapped, int size) {
            this.wrapped = wrapped;
            this.values = new double[size];
        }

        @Override
        public double compute(Context context) {
            if (!(context instanceof GridContext grid)) return wrapped.compute(context);

            int i = grid.index;
            if (i == lastIndex) return lastValue; // shared node
            double v = wrapped.compute(context);
            values[i] = v;
            lastIndex = i;
            lastValue = v;
            filled++;
            return v;
        }

        public double[] values() {
            return values;
        }

        public boolean isComplete() {
            return filled == values.length;
        }

        @Override
        public MapCodec<? extends NoiseFunction> codec() {
            throw new UnsupportedOperationException("Probe trees are transient");
        }

        @Override
        public NoiseFunction mapAll(Visitor visitor) {
            throw new UnsupportedOperationException("Probe trees are transient");
        }

        @Override
        public NoiseFunction mapChildren(UnaryOperator<NoiseFunction> function) {
            throw new UnsupportedOperationException("Probe trees are transient");
        }
    }
}
