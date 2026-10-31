package com.seven.procgen.engine;

import com.seven.procgen.engine.noise.combiner.FBMCombiner;
import com.seven.procgen.engine.noise.combiner.NoiseCombiner;
import com.seven.procgen.engine.noise.combiner.RidgedMultifractalCombiner;
import com.seven.procgen.engine.noise.function.NoiseFunction;
import com.seven.procgen.engine.random.JavaRandomSource;
import com.seven.procgen.engine.random.RandomFork;
import com.seven.procgen.engine.registry.Registry;
import com.seven.procgen.engine.registry.RegistryEntryLookup;

import java.util.Random;

import static com.seven.procgen.engine.noise.combiner.NoiseCombinerType.FBM;
import static com.seven.procgen.engine.noise.combiner.NoiseCombinerType.RIDGED_MULTIFRACTAL;
import static com.seven.procgen.engine.noise.function.NoiseFunctions.createCombiner;
import static com.seven.procgen.engine.noise.function.NoiseFunctions.createSampler;
import static com.seven.procgen.engine.noise.sampler.NoiseSamplerType.PERLIN;

public final class TestNoise {
    public static final int GRID = 32;
    private static final RegistryEntryLookup.RegistryLookup REGISTRY_LOOKUP;

    private TestNoise() {
    }

    static NoiseFunction tree() {
        NoiseFunction base = createSampler("base", PERLIN, 0.1d, 0.1d, 1.0d);
        NoiseFunction ridged = createCombiner("ridged", RIDGED_MULTIFRACTAL, PERLIN,
            RidgedMultifractalCombiner.Parameters.of(5, 0.05d, 2.0d, 0.5d, 2.0d, 1.0d));
        NoiseFunction fbm = createCombiner("fbm", FBM, PERLIN, FBMCombiner.Parameters.of(-2, 1.0d, 1.0d, 1.0d, 1.0d));
        return base.add(ridged.mul(0.5d)).add(fbm.abs());
    }

    static {
        Registry.bootstrap();
        REGISTRY_LOOKUP = Registry.getDefaultLookup();
    }

    static void ensureSetup() {
        // this ensures the static ctor was called
    }

    public static NoiseFunction bind(NoiseFunction tree, long seed) {
        RandomFork fork = new JavaRandomSource(seed).fork();
        return tree.mapAll(new NoiseFunction.Visitor() {
            @Override
            public NoiseFunction apply(NoiseFunction input) {
                return input;
            }

            @Override
            public <T> NoiseFunction.SamplerEntry<T> visitSampler(NoiseFunction.SamplerEntry<T> sampler) {
                return sampler.bind(fork, REGISTRY_LOOKUP);
            }

            @Override
            public <T extends NoiseCombiner.Parameters> NoiseFunction.CombinerEntry<T> visitCombiner(NoiseFunction.CombinerEntry<T> combiner) {
                return combiner.bind(fork, REGISTRY_LOOKUP);
            }
        });
    }

    public static double at(NoiseFunction f, double x, double y, double z) {
        return f.compute(new NoiseFunction.SinglePointContext(x, y, z));
    }

    public static double[] sampleGrid(NoiseFunction f) {
        double[] out = new double[GRID * GRID * GRID];

        for (int i = 0; i < GRID; i++) {
            for (int j = 0; j < GRID; j++) {
                for (int k = 0; k < GRID; k++) {
                    out[(k * GRID + j) * GRID + i] = at(f, i * 3.71d + 0.31d, j * 2.93d + 0.17d, k * 3.17d + 0.23d);
                }
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
            double v = at(f, (points.nextDouble() * 2 - 1) * 2000, (points.nextDouble() * 2 - 1) * 2000, (points.nextDouble() * 2 - 1) * 2000);
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        return new double[]{min, max};
    }
}
