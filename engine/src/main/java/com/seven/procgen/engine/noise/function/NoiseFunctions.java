package com.seven.procgen.engine.noise.function;

import com.seven.procgen.engine.noise.combiner.NoiseCombiner;
import com.seven.procgen.engine.noise.combiner.NoiseCombinerProvider;
import com.seven.procgen.engine.noise.sampler.NoiseSamplerProvider;

public final class NoiseFunctions {

    public static NoiseFunction createSampler(NoiseSamplerProvider provider) {
        return createSampler(provider, 1.0d, 1.0d);
    }

    public static NoiseFunction createSampler(NoiseSamplerProvider provider, double xScale, double yScale) {
        return new Sampler(new NoiseFunction.SamplerEntry(provider, null), xScale, yScale);
    }

    public static <T extends NoiseCombiner.Parameters> NoiseFunction createCombiner(NoiseCombinerProvider<T> provider, NoiseSamplerProvider samplerProvider, T parameters) {
        return createCombiner(provider, samplerProvider, parameters, 1.0d, 1.0d);
    }

    public static <T extends NoiseCombiner.Parameters> NoiseFunction createCombiner(NoiseCombinerProvider<T> provider, NoiseSamplerProvider samplerProvider, T parameters, double xScale, double yScale) {
        return new Combiner(new NoiseFunction.CombinerEntry<>(provider, samplerProvider, parameters, null), xScale, yScale);
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

    public record Combiner<T extends NoiseCombiner.Parameters>(NoiseFunction.CombinerEntry<T> combinerEntry, double xScale, double yScale) implements NoiseFunction {
        @Override
        public double compute(Context context) {
            return combinerEntry.combine(context.x() * xScale, context.y() * yScale);
        }

        @Override
        public NoiseFunction mapAll(Visitor visitor) {
            return visitor.apply(new Combiner(visitor.visitCombiner(this.combinerEntry), xScale, yScale));
        }
    }
}
