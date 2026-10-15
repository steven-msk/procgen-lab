package com.seven.procgen.engine.noise;

import com.seven.procgen.engine.noise.function.NoiseFunction;

public class NoiseField {
    private final int width;
    private final int height;
    private final double[] values;
    private final double min;
    private final double max;
    private final int nonFiniteCount;

    private NoiseField(int width, int height, double[] values, double min, double max, int nonFiniteCount) {
        this.width = width;
        this.height = height;
        this.values = values;
        this.min = min;
        this.max = max;
        this.nonFiniteCount = nonFiniteCount;
    }

    /// Samples pixel (px, py) at world position (originX + px * step, originY + py * step).
    /// Increase y goes down the image, matching Pixmap rows.
    public static NoiseField sample(NoiseFunction function, int width, int height, double originX, double originY, double step) {
        double[] values = new double[width * height];
        PointContext context = new PointContext();
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        int nonFinite = 0;

        for (int y = 0; y < height; y++) {
            context.setY(originY + y * step);
            for (int x = 0; x < width; x++) {
                context.setX(originX + x * step);
                double v = function.compute(context);
                values[y * width + x] = v;
                if (Double.isFinite(v)) {
                    if (v < min) min = v;
                    if (v > max) max = v;
                } else {
                    nonFinite++;
                }
            }
        }

        if (min > max) { // nothing finite
            min = 0.0d;
            max = 0.0d;
        }
        return new NoiseField(width, height, values, min, max, nonFinite);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public double get(int x, int y) {
        return values[y * width + x];
    }

    public double min() {
        return min;
    }

    public double max() {
        return max;
    }

    public int nonFiniteCount() {
        return nonFiniteCount;
    }
}
