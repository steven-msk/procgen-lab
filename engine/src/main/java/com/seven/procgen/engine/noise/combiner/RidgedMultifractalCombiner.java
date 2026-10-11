package com.seven.procgen.engine.noise.combiner;

import com.seven.procgen.engine.noise.sampler.NoiseSampler;
import com.seven.procgen.engine.noise.sampler.NoiseSamplerProvider;
import com.seven.procgen.engine.random.RandomFork;
import com.seven.procgen.engine.random.RandomSource;
import com.seven.procgen.engine.util.math.NoiseMath;

public final class RidgedMultifractalCombiner extends NoiseCombiner<RidgedMultifractalCombiner.Parameters> {
    private final NoiseSampler[] samplers;
    private final double[] amplitudes;
    private final double frequency;
    private final double lacunarity;
    private final double gain;
    private final double offset;
    private final double normalization;

    public RidgedMultifractalCombiner(RandomSource random, NoiseSamplerProvider samplerProvider, Parameters parameters) {
        this(random, samplerProvider, "", parameters);
    }

    public RidgedMultifractalCombiner(RandomSource random, NoiseSamplerProvider samplerProvider, String salt, Parameters parameters) {
        super(random, samplerProvider, parameters);
        this.frequency = parameters.frequency;
        this.lacunarity = parameters.lacunarity;
        this.gain = parameters.gain;
        this.offset = parameters.offset;

        samplers = new NoiseSampler[parameters.octaves];
        amplitudes = new double[parameters.octaves];

        RandomFork fork = random.fork();
        double amplitude = 1.0d;
        double amplitudeSum = 0.0d;
        for (int i = 0; i < parameters.octaves; i++) {
            long seed = fork.fromHashOf(salt + "_octave_" + i).nextLong();
            samplers[i] = samplerProvider.getSampler(seed);
            amplitudes[i] = amplitude;
            amplitudeSum += amplitude;
            amplitude *= parameters.persistence;
        }

        // Largest value a single (unweighted) octave can produce
        double maxSignal = Math.max(offset * offset, (offset - 1.0d) * (offset - 1.0d));
        normalization = 1.0d / (amplitudeSum * maxSignal);
    }

    @Override
    public double compute(double x, double y) {
        double result = 0.0d;
        double weight = 1.0d;
        double f = frequency;

        for (int i = 0; i < samplers.length; i++) {
            double n = samplers[i].sample(NoiseMath.wrap(x * f), NoiseMath.wrap(y * f));

            double signal = offset - Math.abs(n); // fold: creates the ridge at n == 0
            signal *= signal;                     // sharpens the ridge
            signal *= weight;                     // damp by the previous octave

            result += signal * amplitudes[i];

            // high areas of this octave get more detail from the next one
            weight = Math.clamp(signal * gain, 0.0d, 1.0d);
            f *= lacunarity;
        }

        return result * normalization;
    }

    public record Parameters(int octaves, double frequency, double lacunarity, double persistence, double gain, double offset) implements NoiseCombiner.Parameters {
        public static Parameters of(int octaves, double frequency, double lacunarity, double persistence, double gain, double offset) {
            return new Parameters(octaves, frequency, lacunarity, persistence, gain, offset);
        }
    }
}
