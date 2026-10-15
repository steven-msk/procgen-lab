package com.seven.procgen.engine;

import com.seven.procgen.engine.noise.combiner.FBMCombiner;
import com.seven.procgen.engine.noise.combiner.RidgedMultifractalCombiner;
import com.seven.procgen.engine.noise.function.NoiseFunction;
import com.seven.procgen.engine.noise.sampler.NoiseSamplerProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.seven.procgen.engine.TestNoise.*;
import static com.seven.procgen.engine.noise.combiner.NoiseCombinerType.FBM;
import static com.seven.procgen.engine.noise.combiner.NoiseCombinerType.RIDGED_MULTIFRACTAL;
import static com.seven.procgen.engine.noise.function.NoiseFunctions.createCombiner;
import static com.seven.procgen.engine.noise.function.NoiseFunctions.createSampler;
import static com.seven.procgen.engine.noise.sampler.NoiseSamplerType.PERLIN;

public class NoiseRangeTest {
    private static final double EPS = 1e-9d;
    private static final int POINTS = 100_000;
    private static final long[] SEEDS = {0L, 1L, 42L, -7L};

    @Test
    void perlinStaysWithinMinusOneToOne() {
        NoiseFunction perlin = createSampler("perlin", PERLIN);
        for (long seed : SEEDS) {
            double[] r = range(bind(perlin, seed), POINTS, 99L);
            Assertions.assertTrue(r[0] >= -1.0d - EPS, "seed " + seed + ": min was " + r[0]);
            Assertions.assertTrue(r[1] <= 1.0d + EPS, "seed " + seed + ": max was " + r[1]);
            // not degenerate: it should actually use most of the range
            Assertions.assertTrue(r[0] < -0.5d && r[1] > 0.5d, "seed " + seed + ": range was " + r[0] + ".." + r[1]);
        }
    }

    @Test
    void fbmWithDefaultsStaysWithinMinusOneToOne() {
        // Note: the FBM normalization assumes persistence 0.5, so only the defaults are covered here
        NoiseFunction fbm = createCombiner("fbm", FBM, PERLIN,
                FBMCombiner.Parameters.of(-2, 1.0d, 1.0d, 1.0d, 1.0d));
        for (long seed : SEEDS) {
            double[] r = range(bind(fbm, seed), POINTS, 99L);
            Assertions.assertTrue(r[0] >= -1.0d - EPS, "seed " + seed + ": min was " + r[0]);
            Assertions.assertTrue(r[1] <= 1.0d + EPS, "seed " + seed + ": max was " + r[1]);
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
            Assertions.assertTrue(r[0] >= -EPS, "seed " + seed + ": min was " + r[0]);
            Assertions.assertTrue(r[1] <= 1.0d + EPS, "seed " + seed + ": max was " + r[1]);
            Assertions.assertTrue(r[1] > 0.3d, "seed " + seed + ": max was only " + r[1]);
        }
    }

    /// One octave over a constant input has a known answer: ((offset - n)^2) / max(offset^2, (offset - 1)^2).
    /// This catches parameter mix-ups, e.g. offset accidentally read from gain.
    @Test
    void ridgedSingleOctaveMatchesClosedFormValue() {
        NoiseSamplerProvider constantHalf = seed -> (x, y) -> 0.5d;

        NoiseFunction a = createCombiner("a", RIDGED_MULTIFRACTAL, constantHalf,
                RidgedMultifractalCombiner.Parameters.of(1, 1.0d, 2.0d, 0.5d, 2.0d, 1.0d));
        Assertions.assertEquals(0.25d, at(bind(a, 0L), 3.3d, 4.4d), 1e-12d);

        NoiseFunction b = createCombiner("b", RIDGED_MULTIFRACTAL, constantHalf,
                RidgedMultifractalCombiner.Parameters.of(1, 1.0d, 2.0d, 0.5d, 1.5d, 0.8d));
        Assertions.assertEquals(0.09d / 0.64d, at(bind(b, 0L), 3.3d, 4.4d), 1e-12d);
    }
}
