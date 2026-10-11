package com.seven.procgen.engine.noise.combiner;

import com.seven.procgen.engine.noise.sampler.NoiseSampler;
import com.seven.procgen.engine.noise.sampler.NoiseSamplerProvider;
import com.seven.procgen.engine.random.RandomFork;
import com.seven.procgen.engine.random.RandomSource;
import com.seven.procgen.engine.util.math.NoiseMath;
import org.jspecify.annotations.Nullable;

public final class FBMCombiner extends NoiseCombiner<FBMCombiner.Parameters> {
    public static final double DEFAULT_LACUNARITY = 2.0d;
    public static final double DEFAULT_PERSISTENCE = 0.5d;
    private final @Nullable NoiseSampler[] samplerLevels;
    private final double[] octaveMultipliers;
    private final double lacunarity;
    private final double persistence;
    private final double lowestFreqValueFactor;
    private final double lowestFreqInputFactor;

    public FBMCombiner(RandomSource random, NoiseSamplerProvider samplerProvider, Parameters parameters) {
        this(random, samplerProvider, "", parameters);
    }

    public FBMCombiner(RandomSource random, NoiseSamplerProvider samplerProvider, String salt, Parameters parameters) {
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
    public double compute(double x, double y) {
        double value = 0.0d;
        double factor = lowestFreqInputFactor;
        double valueFactor = lowestFreqValueFactor;

        for (int i = 0; i < samplerLevels.length; i++) {
            NoiseSampler sampler = samplerLevels[i];
            if (sampler != null) {
                double noise = sampler.sample(NoiseMath.wrap(x * factor), NoiseMath.wrap(y * factor));
                value += octaveMultipliers[i] * noise * valueFactor;
            }
            factor *= lacunarity;
            valueFactor *= persistence;
        }

        return value;
    }

    public record Parameters(int firstOctave, double[] octaveMultipliers, double lacunarity, double persistence) implements NoiseCombiner.Parameters {
        public static Parameters of(int firstOctave, double... octaveMultipliers) {
            return new Parameters(firstOctave, octaveMultipliers, DEFAULT_LACUNARITY, DEFAULT_PERSISTENCE);
        }

        public Parameters lacunarity(double lacunarity) {
            return new Parameters(firstOctave, octaveMultipliers, lacunarity, persistence);
        }

        public Parameters persistence(double persistence) {
            return new Parameters(firstOctave, octaveMultipliers, lacunarity, persistence);
        }
    }
}
