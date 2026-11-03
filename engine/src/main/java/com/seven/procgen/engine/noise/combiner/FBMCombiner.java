package com.seven.procgen.engine.noise.combiner;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.seven.procgen.engine.noise.graph.FunctionDecompose;
import com.seven.procgen.engine.noise.graph.ParamSpec;
import com.seven.procgen.engine.noise.graph.ParameterDescriptor;
import com.seven.procgen.engine.noise.sampler.NoiseSampler;
import com.seven.procgen.engine.noise.sampler.NoiseSamplerProvider;
import com.seven.procgen.engine.random.RandomFork;
import com.seven.procgen.engine.random.RandomSource;
import com.seven.procgen.engine.util.math.NoiseMath;
import com.seven.procgen.engine.util.serialization.Codecs;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class FBMCombiner extends NoiseCombiner<FBMCombiner.Parameters> {
    public static final double DEFAULT_LACUNARITY = 2.0d;
    public static final double DEFAULT_PERSISTENCE = 0.5d;
    public static final int DEFAULT_FIRST_OCTAVE = -5;
    public static final double[] DEFAULT_OCTAVES = new double[]{1.0d, 1.0d, 1.0d};
    private final @Nullable NoiseSampler[] samplerLevels;
    private final double[] octaveMultipliers;
    private final double lacunarity;
    private final double persistence;
    private final double lowestFreqValueFactor;
    private final double lowestFreqInputFactor;

    public FBMCombiner(RandomSource random, NoiseSamplerProvider<?> samplerProvider, Parameters parameters) {
        this(random, samplerProvider, "", parameters);
    }

    public FBMCombiner(RandomSource random, NoiseSamplerProvider<?> samplerProvider, String salt, Parameters parameters) {
        super(random, samplerProvider, parameters);
        this.octaveMultipliers = parameters.octaveMultipliers;
        this.lacunarity = parameters.lacunarity;
        this.persistence = parameters.persistence;
        int octaves = octaveMultipliers.length;
        int firstOctave = parameters.firstOctave;

        samplerLevels = new NoiseSampler[octaves];
        RandomFork fork = random.fork();
        for (int i = 0; i < octaves; i++) {
            if (octaveMultipliers[i] != 0.0d) {
                long seed = fork.fromHashOf(salt + "_octave_" + i).nextLong();
                NoiseSampler sampler = samplerProvider.getSampler(seed);
                samplerLevels[i] = sampler;
            }
        }
        lowestFreqInputFactor = Math.pow(2.0d, firstOctave);
        lowestFreqValueFactor = Math.pow(2.0d, octaves - 1) / (Math.pow(2.0d, octaves) - 1.0d);
    }

    @Override
    public double compute(double x, double y, double z) {
        double value = 0.0d;
        double factor = lowestFreqInputFactor;
        double valueFactor = lowestFreqValueFactor;

        for (int i = 0; i < samplerLevels.length; i++) {
            NoiseSampler sampler = samplerLevels[i];
            if (sampler != null) {
                double noise = sampler.sample(NoiseMath.wrap(x * factor), NoiseMath.wrap(y * factor), NoiseMath.wrap(z * factor));
                value += octaveMultipliers[i] * noise * valueFactor;
            }
            factor *= lacunarity;
            valueFactor *= persistence;
        }

        return value;
    }

    public record Parameters(int firstOctave, double[] octaveMultipliers, double lacunarity, double persistence) implements NoiseCombiner.Parameters {
        public static final Codec<Parameters> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("first_octave").forGetter(Parameters::firstOctave),
            Codecs.DOUBLE_ARRAY.fieldOf("octave_multipliers").forGetter(Parameters::octaveMultipliers),
            Codec.DOUBLE.fieldOf("lacunarity").forGetter(Parameters::lacunarity),
            Codec.DOUBLE.fieldOf("persistence").forGetter(Parameters::persistence)
        ).apply(i, Parameters::new));
        public static final ParameterDescriptor<Parameters> DESCRIPTOR = ParameterDescriptor.of(
            List.of(
                ParamSpec.rangedInt("first_octave", DEFAULT_FIRST_OCTAVE, -20, 20),
                ParamSpec.doubleArray("octave_multipliers", DEFAULT_OCTAVES, 1, 16),
                ParamSpec.rangedDouble("lacunarity", DEFAULT_LACUNARITY, 1.0d, 8.0d),
                ParamSpec.rangedDouble("persistence", DEFAULT_PERSISTENCE, 0.0d, 1.0d)
            ),
            c -> new Parameters(
                c.param("first_octave"), c.param("octave_multipliers"),
                c.param("lacunarity"), c.param("persistence")
            ),
            p -> FunctionDecompose.of()
                .param("first_octave", p.firstOctave())
                .param("octave_multipliers", p.octaveMultipliers().clone())
                .param("lacunarity", p.lacunarity())
                .param("persistence", p.persistence())
        );
        public static final Parameters DEFAULT = new Parameters(
            DEFAULT_FIRST_OCTAVE, DEFAULT_OCTAVES, DEFAULT_LACUNARITY, DEFAULT_PERSISTENCE
        );

        public Parameters {
            octaveMultipliers = octaveMultipliers.clone();
        }

        public static Parameters of(int firstOctave, double... octaveMultipliers) {
            return new Parameters(firstOctave, octaveMultipliers, DEFAULT_LACUNARITY, DEFAULT_PERSISTENCE);
        }

        public Parameters lacunarity(double lacunarity) {
            return new Parameters(firstOctave, octaveMultipliers, lacunarity, persistence);
        }

        public Parameters persistence(double persistence) {
            return new Parameters(firstOctave, octaveMultipliers, lacunarity, persistence);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Parameters(int otherFirstOctave, double[] otherMultipliers, double otherLacunarity, double otherPersistence)
                && firstOctave == otherFirstOctave
                && Arrays.equals(octaveMultipliers, otherMultipliers)
                && Double.compare(lacunarity, otherLacunarity) == 0
                && Double.compare(persistence, otherPersistence) == 0;
        }

        @Override
        public int hashCode() {
            return 31 * Objects.hash(firstOctave, lacunarity, persistence) + Arrays.hashCode(octaveMultipliers);
        }

        @Override
        public @NonNull String toString() {
            return "Parameters[firstOctave=" + firstOctave + ", octaveMultipliers=" + Arrays.toString(octaveMultipliers)
                + ", lacunarity=" + lacunarity + ", persistence=" + persistence + "]";
        }
    }
}
