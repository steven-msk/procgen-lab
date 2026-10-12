package com.seven.procgen.engine.noise.function;

import com.seven.procgen.engine.noise.combiner.NoiseCombiner;
import com.seven.procgen.engine.noise.combiner.NoiseCombinerProvider;
import com.seven.procgen.engine.noise.sampler.NoiseSamplerProvider;
import com.seven.procgen.engine.registry.Identifier;

public final class NoiseFunctions {

    public static NoiseFunction createSampler(String id, NoiseSamplerProvider provider) {
        return createSampler(id, provider, 1.0d, 1.0d);
    }

    public static NoiseFunction createSampler(String id, NoiseSamplerProvider provider, double xScale, double yScale) {
        return new Sampler(new NoiseFunction.SamplerEntry(Identifier.of(id), provider, null), xScale, yScale);
    }

    public static <T extends NoiseCombiner.Parameters> NoiseFunction createCombiner(
            String id, NoiseCombinerProvider<T> provider, NoiseSamplerProvider samplerProvider, T parameters
    ) {
        return createCombiner(id, provider, samplerProvider, parameters, 1.0d, 1.0d);
    }

    public static <T extends NoiseCombiner.Parameters> NoiseFunction createCombiner(
            String id, NoiseCombinerProvider<T> provider, NoiseSamplerProvider samplerProvider, T parameters, double xScale, double yScale
    ) {
        return new Combiner(new NoiseFunction.CombinerEntry<>(Identifier.of(id), provider, samplerProvider, parameters, null), xScale, yScale);
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
