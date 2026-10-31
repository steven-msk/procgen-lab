package com.seven.procgen.engine;

import com.seven.procgen.engine.noise.sampler.*;
import com.seven.procgen.engine.noise.sampler.WorleyNoise.Metric;
import com.seven.procgen.engine.noise.sampler.WorleyNoise.Output;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/// Sampler cores in isolation. Sampler/Combiner *nodes* just forward to these after binding,
/// so this is the leaf of the determinism argument.
class SamplerDeterminismTest {
    private static final long SEED = 12345L;

    private static Arguments c(String name, Function<Long, NoiseSampler> factory, boolean seedSensitive) {
        return Arguments.of(name, factory, seedSensitive);
    }

    static Stream<Arguments> samplers() {
        List<Arguments> cases = new ArrayList<>();
        cases.add(c("perlin", PerlinNoise::new, true));
        cases.add(c("value", ValueNoise::new, true));
        cases.add(c("simplex", SimplexNoise::new, true));
        for (Output output : Output.values()) {
            cases.add(c("worley " + output, seed -> WorleyNoise.of(seed, output), true));
        }
        for (Metric metric : Metric.values()) {
            cases.add(c("worley F2_MINUS_F1 " + metric, seed -> new WorleyNoise(seed, 0.7d, metric, Output.F2_MINUS_F1), true));
        }
        // jitter 0 puts every feature point at its cell center, so distance outputs ignore the seed
        cases.add(c("worley F1 jitter 0", seed -> new WorleyNoise(seed, 0.0d, Metric.EUCLIDEAN, Output.F1), false));
        cases.add(c("worley CELL jitter 0", seed -> new WorleyNoise(seed, 0.0d, Metric.EUCLIDEAN, Output.CELL), true));
        return cases.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("samplers")
    void sameSeedGivesIdenticalOutput(String name, Function<Long, NoiseSampler> factory, boolean seedSensitive) {
        NoiseSampler a = factory.apply(SEED);
        NoiseSampler b = factory.apply(SEED);
        NoiseDeterminism.assertDeterministic(a::sample, b::sample);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("samplers")
    void differentSeedChangesOutput(String name, Function<Long, NoiseSampler> factory, boolean seedSensitive) {
        if (!seedSensitive) return;
        double[] first = NoiseDeterminism.run(factory.apply(SEED)::sample, NoiseDeterminism.forward());
        double[] second = NoiseDeterminism.run(factory.apply(SEED + 1)::sample, NoiseDeterminism.forward());

        int differing = 0;
        for (int i = 0; i < first.length; i++)
            if (Double.doubleToLongBits(first[i]) != Double.doubleToLongBits(second[i])) differing++;
        Assertions.assertTrue(differing > first.length / 2,
            "only " + differing + " of " + first.length + " samples changed with the seed");
    }
}