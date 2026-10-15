package com.seven.procgen.engine;

import com.seven.procgen.engine.noise.combiner.NoiseCombiner;
import com.seven.procgen.engine.noise.function.NoiseFunction;
import com.seven.procgen.engine.noise.sampler.NoiseSamplerProvider;
import com.seven.procgen.engine.noise.sampler.PerlinNoise;
import com.seven.procgen.engine.random.JavaRandomSource;
import com.seven.procgen.engine.random.RandomFork;

import java.util.Random;

public final class TestNoise {
    private static final NoiseSamplerProvider PERLIN = PerlinNoise::new;
    public static final int GRID = 32;

    private TestNoise() {
    }

    public static NoiseFunction bind(NoiseFunction tree, long seed) {
        RandomFork fork = new JavaRandomSource(seed).fork();
        return tree.mapAll(new NoiseFunction.Visitor() {
            @Override
            public NoiseFunction apply(NoiseFunction input) {
                return input;
            }

            @Override
            public NoiseFunction.SamplerEntry visitSampler(NoiseFunction.SamplerEntry sampler) {
                return sampler.bind(fork);
            }

            @Override
            public <T extends NoiseCombiner.Parameters> NoiseFunction.CombinerEntry<T> visitCombiner(NoiseFunction.CombinerEntry<T> combiner) {
                return combiner.bind(fork);
            }
        });
    }

    public static double at(NoiseFunction f, double x, double y) {
        return f.compute(new NoiseFunction.SinglePointContext(x, y));
    }

    public static double[] sampleGrid(NoiseFunction f) {
        double[] out = new double[GRID * GRID];
        for (int i = 0; i < GRID; i++) {
            for (int j = 0; j < GRID; j++) {
                out[j * GRID + i] = at(f, i * 3.71d + 0.31d, j * 2.93d + 0.17d);
            }
        }
        return out;
    }

    public static int countDifferences(double[] a, double[] b) {
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) diff++;
        }
        return diff;
    }

    /// {min, max} over random points in [-2000, 2000]^2.
    public static double[] range(NoiseFunction f, int count, long pointSeed) {
        Random points = new Random(pointSeed);
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < count; i++) {
            double v = at(f, (points.nextDouble() * 2 - 1) * 2000, (points.nextDouble() * 2 - 1) * 2000);
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        return new double[]{min, max};
    }
}
