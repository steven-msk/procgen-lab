package com.seven.procgen.engine.noise.data;

/// Fraction of finite sampler per bin over [minInclusive, maxInclusive], and the fractions that fell outside of it
public record Histogram(float[] fractions, float peak, double min, double max, float below, float above) {
    public static Histogram of(SampleField field, double min, double max, int binCount) {
        int[] counts = new int[binCount];
        int below = 0;
        int above = 0;
        int total = 0;
        double span = max - min;
        for (int y = 0; y < field.height(); y++) {
            for (int x = 0; x < field.width(); x++) {
                double v = field.get(x, y);
                if (!Double.isFinite(v)) continue;
                total++;
                if (!(span > 0.0d)) {
                    counts[binCount / 2]++; // degenerate range (constant field)
                } else if (v < min) {
                    below++;
                } else if (v > max) {
                    above++;
                } else {
                    counts[Math.min((int)((v - min) / span * binCount), binCount - 1)]++; // v == maxInclusive lands in the last bin
                }
            }
        }
        float inv = total > 0 ? 1.0f / total : 0.0f;
        float[] fractions = new float[binCount];
        float peak = 0.0f;
        for (int i = 0; i < binCount; i++) {
            fractions[i] = counts[i] * inv;
            peak = Math.max(peak, fractions[i]);
        }
        return new Histogram(fractions, peak, min, max, below * inv, above * inv);
    }
}
