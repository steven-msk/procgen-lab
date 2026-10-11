package com.seven.procgen.engine.noise.combiner;

import com.seven.procgen.engine.noise.function.NoiseFunction;
import com.seven.procgen.engine.noise.function.NoiseSampler;
import com.seven.procgen.engine.random.RandomFork;
import com.seven.procgen.engine.random.RandomSource;
import org.jspecify.annotations.Nullable;

public final class FBMCombiner implements NoiseFunction {
    public static final double DEFAULT_LACUNARITY = 2.0d;
    public static final double DEFAULT_PERSISTENCE = 0.5d;
    private final @Nullable NoiseSampler[] samplerLevels;
    private final double[] octaveMultipliers;
    private final double lacunarity;
    private final double persistence;
    private final double lowestFreqValueFactor;
    private final double lowestFreqInputFactor;

    public FBMCombiner(RandomSource random, NoiseFunction function, Parameters parameters) {
        this(random, function, "", parameters);
    }

    public FBMCombiner(RandomSource random, NoiseFunction function, String salt, Parameters parameters) {
        this(random, function, salt, parameters.firstOctave, parameters.octaveMultipliers, parameters.lacunarity, parameters.persistence);
    }

    public FBMCombiner(
            RandomSource random, NoiseFunction noiseFunction, String salt,
            int firstOctave, double[] octaveMultipliers, double lacunarity, double persistence
    ) {
        this.octaveMultipliers = octaveMultipliers;
        this.lacunarity = lacunarity;
        this.persistence = persistence;
        int octaves = octaveMultipliers.length;

        samplerLevels = new NoiseSampler[octaves];
        RandomFork fork = random.fork();
        for (int i = 0; i < octaves; i++) {
            if (octaveMultipliers[i] != 0.0d) {
                long seed = fork.fromHashOf(salt + "_octave_" + i).nextLong();
//                NoiseSampler sampler = noiseFunction.createSampler(seed);
//                samplerLevels[i] = sampler;
            }
        }
        lowestFreqInputFactor = Math.pow(2.0d, firstOctave);
        lowestFreqValueFactor = Math.pow(2.0d, octaves - 1) / (Math.pow(2.0d, octaves) - 1.0d);
    }

    @Override
    public double compute(Context context) {
        return 0;
    }

    @Override
    public NoiseFunction mapAll(Visitor visitor) {
        return null;
    }

//    @Override
//    public NoiseSampler compile(Compiler compiler) {
//        return (x, y) -> {
//            double value = 0.0d;
//            double factor = lowestFreqInputFactor;
//            double valueFactor = lowestFreqValueFactor;
//
//            for (int i = 0; i < samplerLevels.length; i++) {
//                NoiseSampler sampler = samplerLevels[i];
//                if (sampler != null) {
//                    double noise = sampler.sample(NoiseMath.wrap(x * factor), NoiseMath.wrap(y * factor));
//                    value += octaveMultipliers[i] * noise * valueFactor;
//                }
//                factor *= lacunarity;
//                valueFactor *= persistence;
//            }
//
//            return value;
//        };
//    }

    public record Parameters(int firstOctave, double[] octaveMultipliers, double lacunarity, double persistence) {
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
