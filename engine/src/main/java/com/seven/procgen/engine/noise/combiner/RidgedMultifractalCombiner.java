package com.seven.procgen.engine.noise.combiner;

import com.seven.procgen.engine.noise.function.NoiseFunction;
import com.seven.procgen.engine.noise.function.NoiseSampler;
import com.seven.procgen.engine.random.RandomFork;
import com.seven.procgen.engine.random.RandomSource;
import com.seven.procgen.engine.util.math.NoiseMath;

public final class RidgedMultifractalCombiner implements NoiseSampler {
    private final NoiseSampler[] samplers;
    private final double[] amplitudes;
    private final double frequency;
    private final double lacunarity;
    private final double gain;
    private final double offset;
    private final double normalization;

    public RidgedMultifractalCombiner(
            RandomSource random, NoiseFunction noiseFunction, String salt,
            int octaves, double frequency, double lacunarity,
            double persistence, double gain, double offset
    ) {
        this.frequency = frequency;
        this.lacunarity = lacunarity;
        this.gain = gain;
        this.offset = offset;

        samplers = new NoiseSampler[octaves];
        amplitudes = new double[octaves];

        RandomFork fork = random.fork();
        double amplitude = 1.0d;
        double amplitudeSum = 0.0d;
        for (int i = 0; i < octaves; i++) {
            long seed = fork.fromHashOf(salt + "_octave_" + i).nextLong();
            samplers[i] = noiseFunction.createSampler(seed);
            amplitudes[i] = amplitude;
            amplitudeSum += amplitude;
            amplitude *= persistence;
        }

        // Largest value a single (unweighted) octave can produce
        double maxSignal = Math.max(offset * offset, (offset - 1.0d) * (offset - 1.0d));
        normalization = 1.0d / (amplitudeSum * maxSignal);
    }

    @Override
    public double getNoise(double x, double y) {
        double result = 0.0d;
        double weight = 1.0d;
        double f = frequency;

        for (int i = 0; i < samplers.length; i++) {
            double n = samplers[i].getNoise(NoiseMath.wrap(x * f), NoiseMath.wrap(y * f));

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
}
