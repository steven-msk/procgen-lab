package com.seven.procgen.engine;

import com.seven.procgen.engine.noise.combiner.FBMCombiner;
import com.seven.procgen.engine.noise.combiner.RidgedMultifractalCombiner;
import com.seven.procgen.engine.noise.data.NoiseProbes;
import com.seven.procgen.engine.noise.data.SampleField;
import com.seven.procgen.engine.noise.function.NoiseFunction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.seven.procgen.engine.TestNoise.*;
import static com.seven.procgen.engine.noise.combiner.NoiseCombinerType.FBM;
import static com.seven.procgen.engine.noise.combiner.NoiseCombinerType.RIDGED_MULTIFRACTAL;
import static com.seven.procgen.engine.noise.function.NoiseFunctions.createCombiner;
import static com.seven.procgen.engine.noise.function.NoiseFunctions.createSampler;
import static com.seven.procgen.engine.noise.sampler.NoiseSamplerType.PERLIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class NoiseDeterminismTest {
    static NoiseFunction tree() {
        NoiseFunction base = createSampler("base", PERLIN, 0.1d, 0.1d, 1.0d);
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
        Assertions.assertThrows(IllegalStateException.class, () -> at(tree, 0.5d, 0.5d, 0.5d));

        bind(tree, 1L);

        Assertions.assertThrows(IllegalStateException.class, () -> at(tree, 0.5d, 0.5d, 0.5d));
    }

    @Test
    void probedFieldsMatchDirectSampling() {
        NoiseFunction root = bind(tree(), 1L);
        Map<NoiseFunction, SampleField> probed = NoiseProbes.sampleAll(root, 64, 64, 0.0d, 0.0d, 1.0d);
        assertFalse(probed.isEmpty());
        probed.forEach((node, field) -> {
            SampleField direct = SampleField.sampleFunction(node, 64, 64, 0.0d, 0.0d, 1.0d);
            for (int y = 0; y < 64; y++) {
                for (int x = 0; x < 64; x++) {
                    assertEquals(direct.get(x, y), field.get(x, y), 0.0, node + " @ " + x + "," + y);
                }
            }
        });
    }
}
