package com.seven.procgen.engine;

import com.seven.procgen.engine.noise.combiner.FBMCombiner;
import com.seven.procgen.engine.noise.combiner.RidgedMultifractalCombiner;
import com.seven.procgen.engine.noise.function.NoiseFunction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.seven.procgen.engine.TestNoise.bind;
import static com.seven.procgen.engine.TestNoise.range;
import static com.seven.procgen.engine.noise.combiner.NoiseCombinerType.FBM;
import static com.seven.procgen.engine.noise.combiner.NoiseCombinerType.RIDGED_MULTIFRACTAL;
import static com.seven.procgen.engine.noise.function.NoiseFunctions.createCombiner;
import static com.seven.procgen.engine.noise.function.NoiseFunctions.createSampler;
import static com.seven.procgen.engine.noise.sampler.NoiseSamplerType.PERLIN;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NoiseRangeTest {
    private static final double EPS = 1e-9d;
    private static final int POINTS = 100_000;
    private static final long[] SEEDS = {0L, 1L, 42L, -7L};

    @Test
    void perlinStaysWithinMinusOneToOne() {
        NoiseFunction perlin = createSampler("perlin", PERLIN);
        for (long seed : SEEDS) {
            double[] r = range(bind(perlin, seed), POINTS, 99L);
            assertTrue(r[0] >= -1.0d - EPS, "seed " + seed + ": minInclusive was " + r[0]);
            assertTrue(r[1] <= 1.0d + EPS, "seed " + seed + ": maxInclusive was " + r[1]);
            // not degenerate: it should actually use most of the range
            assertTrue(r[0] < -0.5d && r[1] > 0.5d, "seed " + seed + ": range was " + r[0] + ".." + r[1]);
        }
    }

    @Test
    void fbmWithDefaultsStaysWithinMinusOneToOne() {
        // Note: the FBM normalization assumes persistence 0.5, so only the defaults are covered here
        NoiseFunction fbm = createCombiner("fbm", FBM, PERLIN,
            FBMCombiner.Parameters.of(-2, 1.0d, 1.0d, 1.0d, 1.0d));
        for (long seed : SEEDS) {
            double[] r = range(bind(fbm, seed), POINTS, 99L);
            assertTrue(r[0] >= -1.0d - EPS, "seed " + seed + ": minInclusive was " + r[0]);
            assertTrue(r[1] <= 1.0d + EPS, "seed " + seed + ": maxInclusive was " + r[1]);
        }
    }

    static Stream<RidgedMultifractalCombiner.Parameters> ridgedParameters() {
        return Stream.of(
            RidgedMultifractalCombiner.Parameters.of(6, 0.01d, 2.0d, 0.5d, 2.0d, 1.0d),
            RidgedMultifractalCombiner.Parameters.of(4, 0.05d, 2.5d, 0.6d, 1.5d, 0.8d),
            RidgedMultifractalCombiner.Parameters.of(8, 0.02d, 2.0d, 0.5d, 2.0d, 1.2d)
        );
    }

    @ParameterizedTest
    @MethodSource("ridgedParameters")
    void ridgedStaysWithinZeroToOne(RidgedMultifractalCombiner.Parameters parameters) {
        NoiseFunction ridged = createCombiner("ridged", RIDGED_MULTIFRACTAL, PERLIN, parameters);
        for (long seed : SEEDS) {
            double[] r = range(bind(ridged, seed), POINTS, 99L);
            assertTrue(r[0] >= -EPS, "seed " + seed + ": minInclusive was " + r[0]);
            assertTrue(r[1] <= 1.0d + EPS, "seed " + seed + ": maxInclusive was " + r[1]);
            assertTrue(r[1] > 0.3d, "seed " + seed + ": maxInclusive was only " + r[1]);
        }
    }
}
