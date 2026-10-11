package com.seven.procgen.engine.noise.function;

import java.util.function.Function;

public final class NoiseFunctions {

    public static NoiseFunction createSampler(Function<Long, NoiseSampler> factory) {
        return createSampler(factory, 1.0d, 1.0d);
    }

    public static NoiseFunction createSampler(Function<Long, NoiseSampler> factory, double xScale, double yScale) {
        return new Sampler(new NoiseFunction.SamplerEntry(factory, null), xScale, yScale);
    }

    public record Sampler(NoiseFunction.SamplerEntry samplerEntry, double xScale, double yScale) implements NoiseFunction {
        @Override
        public double compute(Context context) {
            return samplerEntry.get(context.x() * xScale, context.y() * yScale);
        }

        @Override
        public NoiseFunction mapAll(Visitor visitor) {
            return visitor.apply(new Sampler(visitor.visitSampler(this.samplerEntry), this.xScale, this.yScale));
        }
    }
}
