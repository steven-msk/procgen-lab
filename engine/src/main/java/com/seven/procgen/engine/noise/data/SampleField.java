package com.seven.procgen.engine.noise.data;

import com.seven.procgen.engine.noise.function.NoiseFunction;

public interface SampleField {
    int width();

    int height();

    double get(int x, int y);

    double min();

    double max();

    int nonFiniteCount();

    static SampleField direct(int width, int height, double[] values) {
        return DirectValues.of(width, height, values);
    }

    static SampleField sampleFunction(NoiseFunction function, int width, int height, double originX, double originY, double step) {
        return SampledFunction.of(function, width, height, originX, originY, step);
    }

    record DirectValues(int width, int height, double[] values, double min, double max, int nonFiniteCount) implements SampleField {
        static DirectValues of(int width, int height, double[] values) {
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            int nonFinite = 0;
            for (double v : values) {
                if (Double.isFinite(v)) {
                    if (v < min) min = v;
                    if (v > max) max = v;
                } else {
                    nonFinite++;
                }
            }
            return new DirectValues(width, height, values.clone(), min, max, nonFinite);
        }

        @Override
        public double get(int x, int y) {
            return values[width * y + x];
        }
    }

    record SampledFunction(int width, int height, double[] values, double min, double max, int nonFiniteCount) implements SampleField {
        static SampledFunction of(NoiseFunction function, int width, int height, double originX, double originY, double step) {
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
            return new SampledFunction(width, height, values, min, max, nonFinite);
        }

        @Override
        public double get(int x, int y) {
            return values[y * width + x];
        }
    }

    record Stats(double min, double max, double mean, double stdDeviation, int nonFinite) {
        public static SampleField.Stats of(SampleField field) {
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            double mean = 0.0d;
            double m2 = 0.0d; // Welford
            int n = 0;
            int bad = 0;

            for (int y = 0; y < field.height(); y++) {
                for (int x = 0; x < field.width(); x++) {
                    double v = field.get(x, y);
                    if (!Double.isFinite(v)) {
                        bad++;
                        continue;
                    }
                    n++;
                    min = Math.min(min, v);
                    max = Math.max(max, v);
                    double d = v - mean;
                    mean += d / n;
                    m2 += d * (v - mean);
                }
            }
            return n == 0
                ? new SampleField.Stats(Double.NaN, Double.NaN, Double.NaN, Double.NaN, bad)
                : new SampleField.Stats(min, max, mean, Math.sqrt(m2 / n), bad);
        }
    }
}
