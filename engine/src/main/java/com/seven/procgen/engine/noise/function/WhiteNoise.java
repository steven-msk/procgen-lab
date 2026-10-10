package com.seven.procgen.engine.noise.function;

public class WhiteNoise implements NoiseFunction {
    @Override
    public NoiseSampler createSampler(NoiseSampler.Parameters parameters) {
        final long seed = parameters.getValueOrThrow(NoiseParameter.SEED);
        final double frequency = parameters.getValue(NoiseParameter.FREQUENCY, 1.0d);

        return (x, y) -> {
            long h = seed
                    ^ ((long)Math.floor(x * frequency) * 0x9E3779B97F4A7C15L)
                    ^ ((long)Math.floor(y * frequency) * 0xC2B2AE3D27D4EB4FL);
            h ^= h >>> 33;
            h *= 0xff51afd7ed558ccdL;
            h ^= h >>> 33;
            return (h >>> 11) * 0x1.0p-53;
        };
    }
}
