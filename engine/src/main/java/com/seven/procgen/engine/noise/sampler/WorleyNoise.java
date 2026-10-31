package com.seven.procgen.engine.noise.sampler;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.seven.procgen.engine.util.math.NoiseMath;
import com.seven.procgen.engine.util.serialization.Codecs;

/// Worley / cellular noise: one jittered feature point per unit cell, output derived from the nearest
/// feature points. 3D, searches the 27 surrounding cells (exact for F1, near-exact for F2 at jitter <= 1).
/// Output is mapped into [-1, 1].
/// Distance outputs use approximate normalization constants and are clamped
public final class WorleyNoise implements NoiseSampler {
    public enum Metric {EUCLIDEAN, MANHATTAN, CHEBYSHEV}

    /// F1 = distance to nearest point, F2 = to second nearest, CELL = flat random value per cell
    public enum Output {F1, F2, F2_MINUS_F1, CELL}

    private static final double INV_21_BITS = 1.0d / 0x200000;
    private static final long CELL_SALT = 0x2545F4914F6CDD1DL;

    private final long seed;
    private final double jitter;
    private final Metric metric;
    private final Output output;

    public WorleyNoise(long seed, double jitter, Metric metric, Output output) {
        this.seed = seed;
        this.jitter = Math.clamp(jitter, 0.0d, 1.0d);
        this.metric = metric;
        this.output = output;

    }

    public WorleyNoise(long seed, WorleyNoise.Parameters parameters) {
        this(seed, parameters.jitter, parameters.metric, parameters.output);
    }

    public static WorleyNoise of(long seed, Output output) {
        return new WorleyNoise(seed, 1.0d, Metric.EUCLIDEAN, output);
    }

    @Override
    public double sample(double x, double y, double z) {
        long cx = NoiseMath.floor(x);
        long cy = NoiseMath.floor(y);
        long cz = NoiseMath.floor(z);
        double fx = x - cx;
        double fy = y - cy;
        double fz = z - cz;

        double f1 = Double.MAX_VALUE;
        double f2 = Double.MAX_VALUE;
        long nearest = 0L;

        for (int dz = -1; dz <= 1; dz++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    long h = NoiseMath.hash(seed, cx + dx, cy + dy, cz + dz);
                    // three 21-bit fields out of one hash
                    double px = dx + 0.5d + ((h & 0x1FFFFFL) * INV_21_BITS - 0.5d) * jitter;
                    double py = dy + 0.5d + (((h >>> 21) & 0x1FFFFFL) * INV_21_BITS - 0.5d) * jitter;
                    double pz = dz + 0.5d + (((h >>> 42) & 0x1FFFFFL) * INV_21_BITS - 0.5d) * jitter;
                    double d = distance(px - fx, py - fy, pz - fz);
                    if (d < f1) {
                        f2 = f1;
                        f1 = d;
                        nearest = h;
                    } else if (d < f2) {
                        f2 = d;
                    }
                }
            }
        }

        if (output == Output.CELL) return NoiseMath.signed(NoiseMath.mix(nearest ^ CELL_SALT));

        if (metric == Metric.EUCLIDEAN) {
            // distance() returned squared values
            f1 = Math.sqrt(f1);
            f2 = Math.sqrt(f2);
        }
        return switch (output) {
            case F1 -> normalise(f1, 1.0d);
            case F2 -> normalise(f2, 1.5d);
            case F2_MINUS_F1 -> normalise(f2 - f1, 1.0d);
            // handled above
            default -> throw new IllegalStateException("Unexpected value: " + output);
        };
    }

    private double distance(double dx, double dy, double dz) {
        return switch (metric) {
            case EUCLIDEAN -> dx * dx + dy * dy + dz * dz;
            case MANHATTAN -> Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
            case CHEBYSHEV -> Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        };
    }

    private static double normalise(double d, double max) {
        double t = d / max;
        return (t < 0.0d ? 0.0d : Math.min(t, 1.0d)) * 2.0d - 1.0d;
    }

    public record Parameters(double jitter, Metric metric, Output output) {
        public static final Parameters DEFAULT = new Parameters(1.0d, Metric.EUCLIDEAN, Output.F1);
        public static final Codec<Parameters> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.doubleRange(0.0d, 1.0d).optionalFieldOf("jitter", 1.0d).forGetter(Parameters::jitter),
            Codecs.enumOf(Metric.class).optionalFieldOf("metric", Metric.EUCLIDEAN).forGetter(Parameters::metric),
            Codecs.enumOf(Output.class).optionalFieldOf("output", Output.F1).forGetter(Parameters::output)
        ).apply(i, Parameters::new));
    }
}