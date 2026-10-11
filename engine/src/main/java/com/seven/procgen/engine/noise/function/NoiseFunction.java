package com.seven.procgen.engine.noise.function;

import com.seven.procgen.engine.noise.combiner.NoiseCombiner;
import com.seven.procgen.engine.noise.combiner.NoiseCombinerProvider;
import com.seven.procgen.engine.noise.sampler.NoiseSampler;
import com.seven.procgen.engine.noise.sampler.NoiseSamplerProvider;
import org.jspecify.annotations.Nullable;

public interface NoiseFunction {
    double compute(Context context);

    NoiseFunction mapAll(Visitor visitor);

    interface Visitor {
        NoiseFunction apply(NoiseFunction input);

        default SamplerEntry visitSampler(SamplerEntry sampler) {
            return sampler;
        }

        default <T extends NoiseCombiner.Parameters> CombinerEntry<T> visitCombiner(CombinerEntry<T> combiner) {
            return combiner;
        }
    }

    interface Context {
        double x();

        double y();
    }

    record SinglePointContext(double x, double y) implements Context {
    }

    record SamplerEntry(NoiseSamplerProvider provider, @Nullable NoiseSampler sampler) {
        public double get(double x, double y) {
            if (sampler == null) throw new IllegalStateException("Compute called with null sampler");
            return sampler.sample(x, y);
        }
    }

    record CombinerEntry<T extends NoiseCombiner.Parameters>(NoiseCombinerProvider<T> provider, NoiseSamplerProvider samplerProvider, T parameters, @Nullable NoiseCombiner<T> combiner) {
        public double combine(double x, double y) {
            if (combiner == null) throw new IllegalStateException("Combine called with null combiner");
            return combiner.compute(x, y);
        }
    }
}
