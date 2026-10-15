package com.seven.procgen.engine;

import com.seven.procgen.engine.noise.combiner.FBMCombiner;
import com.seven.procgen.engine.noise.combiner.RidgedMultifractalCombiner;
import com.seven.procgen.engine.noise.function.NoiseFunction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.seven.procgen.engine.TestNoise.*;
import static com.seven.procgen.engine.noise.combiner.NoiseCombinerType.FBM;
import static com.seven.procgen.engine.noise.combiner.NoiseCombinerType.RIDGED_MULTIFRACTAL;
import static com.seven.procgen.engine.noise.function.NoiseFunctions.createCombiner;
import static com.seven.procgen.engine.noise.function.NoiseFunctions.createSampler;
import static com.seven.procgen.engine.noise.sampler.NoiseSamplerType.PERLIN;

public class NoiseDeterminismTest {
    private static NoiseFunction tree() {
        NoiseFunction base = createSampler("base", PERLIN, 0.1d, 0.1d);
        NoiseFunction ridged = createCombiner("ridged", RIDGED_MULTIFRACTAL, PERLIN,
                RidgedMultifractalCombiner.Parameters.of(5, 0.05d, 2.0d, 0.5d, 2.0d, 1.0d));
        NoiseFunction fbm = createCombiner("fbm", FBM, PERLIN, FBMCombiner.Parameters.of(-2, 1.0d, 1.0d, 1.0d, 1.0d));
        return base.add(ridged.mul(0.5d)).add(fbm.abs());
    }

    @Test
    void bindingTheSameTreeTwiceGivesIdenticalOutput() {
        NoiseFunction tree = tree();
        Assertions.assertArrayEquals(sampleGrid(bind(tree, 42L)), sampleGrid(bind(tree, 42L)));
    }

    @Test
    void bindingAnEquivalentTreeBuiltSeparatelyGivesIdenticalOutput() {
        Assertions.assertArrayEquals(sampleGrid(bind(tree(), 42L)), sampleGrid(bind(tree(), 42L)));
    }

    @Test
    void differentSeedsGiveDifferentOutput() {
        NoiseFunction tree = tree();
        int different = countDifferences(sampleGrid(bind(tree, 1L)), sampleGrid(bind(tree, 2L)));
        Assertions.assertTrue(different > GRID * GRID * 0.9d, "only " + different + " points differed");
    }

    @Test
    void unboundTreeThrowsAndBindingDoesNotMutateIt() {
        NoiseFunction tree = tree();
        Assertions.assertThrows(IllegalStateException.class, () -> at(tree, 0.5d, 0.5d));

        bind(tree, 1L);

        Assertions.assertThrows(IllegalStateException.class, () -> at(tree, 0.5d, 0.5d));
    }
}
