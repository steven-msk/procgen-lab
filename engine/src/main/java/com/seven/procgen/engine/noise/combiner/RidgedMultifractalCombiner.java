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

import java.util.List;

public final class RidgedMultifractalCombiner extends NoiseCombiner<RidgedMultifractalCombiner.Parameters> {
    public static final int DEFAULT_OCTAVES = 6;
    public static final double DEFAULT_FREQUENCY = 1.0d;
    public static final double DEFAULT_LACUNARITY = 2.0d;
    public static final double DEFAULT_PERSISTENCE = 0.5d;
    public static final double DEFAULT_GAIN = 2.0d;
    public static final double DEFAULT_OFFSET = 1.0d;
    private final NoiseSampler[] samplers;
    private final double[] amplitudes;
    private final double frequency;
    private final double lacunarity;
    private final double gain;
    private final double offset;
    private final double normalization;

    public RidgedMultifractalCombiner(RandomSource random, NoiseSamplerProvider<?> samplerProvider, Parameters parameters) {
        this(random, samplerProvider, "", parameters);
    }

    public RidgedMultifractalCombiner(RandomSource random, NoiseSamplerProvider<?> samplerProvider, String salt, Parameters parameters) {
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
    public double compute(double x, double y, double z) {
        double result = 0.0d;
        double weight = 1.0d;
        double f = frequency;

        for (int i = 0; i < samplers.length; i++) {
            double n = samplers[i].sample(NoiseMath.wrap(x * f), NoiseMath.wrap(y * f), NoiseMath.wrap(z * f));

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
        public static final Codec<Parameters> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("octaves").forGetter(Parameters::octaves),
            Codec.DOUBLE.fieldOf("frequency").forGetter(Parameters::frequency),
            Codec.DOUBLE.fieldOf("lacunarity").forGetter(Parameters::lacunarity),
            Codec.doubleRange(0.0d, 1.0d).fieldOf("persistence").forGetter(Parameters::persistence),
            Codec.DOUBLE.fieldOf("gain").forGetter(Parameters::gain),
            Codec.DOUBLE.fieldOf("offset").forGetter(Parameters::offset)
        ).apply(i, Parameters::new));
        public static final ParameterDescriptor<Parameters> DESCRIPTOR = ParameterDescriptor.of(
            List.of(
                ParamSpec.rangedInt("octaves", DEFAULT_OCTAVES, 1, 16),
                ParamSpec.rangedDouble("frequency", DEFAULT_FREQUENCY, 0.0d, 64.0d),
                ParamSpec.rangedDouble("lacunarity", DEFAULT_LACUNARITY, 1.0d, 8.0d),
                ParamSpec.rangedDouble("persistence", DEFAULT_PERSISTENCE, 0.0d, 1.0d),
                ParamSpec.rangedDouble("gain", DEFAULT_GAIN, 0.0d, 8.0d),
                ParamSpec.rangedDouble("offset", DEFAULT_OFFSET, 0.0d, 4.0d)
            ),
            c -> new Parameters(
                c.param("octaves"), c.param("frequency"), c.param("lacunarity"),
                c.param("persistence"), c.param("gain"), c.param("offset")
            ),
            p -> FunctionDecompose.of()
                .param("octaves", p.octaves())
                .param("frequency", p.frequency())
                .param("lacunarity", p.lacunarity())
                .param("persistence", p.persistence())
                .param("gain", p.gain())
                .param("offset", p.offset())
        );
        public static final Parameters DEFAULT = new Parameters(
            DEFAULT_OCTAVES, DEFAULT_FREQUENCY, DEFAULT_LACUNARITY, DEFAULT_PERSISTENCE, DEFAULT_GAIN, DEFAULT_OFFSET
        );

        public static Parameters of(int octaves, double frequency, double lacunarity, double persistence, double gain, double offset) {
            return new Parameters(octaves, frequency, lacunarity, persistence, gain, offset);
        }
    }
}
