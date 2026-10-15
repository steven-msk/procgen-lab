package com.seven.procgen.engine;

import com.seven.procgen.engine.noise.combiner.RidgedMultifractalCombiner;
import com.seven.procgen.engine.noise.function.NoiseFunction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.seven.procgen.engine.TestNoise.*;
import static com.seven.procgen.engine.noise.combiner.NoiseCombinerType.RIDGED_MULTIFRACTAL;
import static com.seven.procgen.engine.noise.function.NoiseFunctions.createCombiner;
import static com.seven.procgen.engine.noise.function.NoiseFunctions.createSampler;
import static com.seven.procgen.engine.noise.sampler.NoiseSamplerType.PERLIN;

public class NoiseKeyIdentityTest {
    private static final long SEED = 7L;
    private static final RidgedMultifractalCombiner.Parameters RIDGED_PARAMS = RidgedMultifractalCombiner.Parameters.of(
            4, 0.05d, 2.0d, 0.5d, 2.0d, 1.0d
    );

    private static NoiseFunction ridged(String id) {
        return createCombiner(id, RIDGED_MULTIFRACTAL, PERLIN, RIDGED_PARAMS);
    }

    @Test
    void sameKeyGivesTheSameNoise() {
        Assertions.assertArrayEquals(
                sampleGrid(bind(createSampler("x", PERLIN), SEED)),
                sampleGrid(bind(createSampler("x", PERLIN), SEED)));
        Assertions.assertArrayEquals(
                sampleGrid(bind(ridged("x"), SEED)),
                sampleGrid(bind(ridged("x"), SEED)));
    }

    @Test
    void differentKeysGiveDifferentNoiseForSamplers() {
        double[] a = sampleGrid(bind(createSampler("a", PERLIN), SEED));
        double[] b = sampleGrid(bind(createSampler("b", PERLIN), SEED));
        Assertions.assertTrue(countDifferences(a, b) > GRID * GRID * 0.9d);
    }

    @Test
    void differentKeysGiveDifferentNoiseForCombiners() {
        double[] a = sampleGrid(bind(ridged("a"), SEED));
        double[] b = sampleGrid(bind(ridged("b"), SEED));
        Assertions.assertTrue(countDifferences(a, b) > GRID * GRID * 0.9d);
    }

    @Test
    void reorderingSiblingsDoesNotChangeResults() {
        NoiseFunction a = createSampler("a", PERLIN, 0.1d, 0.1d);
        NoiseFunction b = ridged("b");

        // a is bound before b in one tree and after b in the other
        Assertions.assertArrayEquals(
                sampleGrid(bind(a.add(b), SEED)),
                sampleGrid(bind(b.add(a), SEED)));
    }

    @Test
    void aNodesNoiseDoesNotDependOnWhatElseIsInTheTree() {
        NoiseFunction a = createSampler("a", PERLIN, 0.1d, 0.1d);
        NoiseFunction b = ridged("b");

        double[] together = sampleGrid(bind(a.add(b), SEED));
        double[] aAlone = sampleGrid(bind(a, SEED));
        double[] bAlone = sampleGrid(bind(b, SEED));

        for (int i = 0; i < together.length; i++) {
            Assertions.assertEquals(aAlone[i] + bAlone[i], together[i], "point " + i);
        }
    }
}
