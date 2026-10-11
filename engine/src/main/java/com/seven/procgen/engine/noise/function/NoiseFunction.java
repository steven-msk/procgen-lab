package com.seven.procgen.engine.noise.function;

import org.jspecify.annotations.Nullable;

import java.util.function.Function;

public interface NoiseFunction {
    double compute(Context context);

    NoiseFunction mapAll(Visitor visitor);

    interface Visitor {
        NoiseFunction apply(NoiseFunction input);

        default SamplerEntry visitSampler(SamplerEntry sampler) {
            return sampler;
        }
    }

    interface Context {
        double x();

        double y();
    }

    public record SinglePointContext(double x, double y) implements Context {
    }

    record SamplerEntry(Function<Long, NoiseSampler> samplerFactory, @Nullable NoiseSampler sampler) {
        public double get(double x, double y) {
            if (sampler == null) throw new IllegalStateException("Compute called with null sampler");
            return sampler.sample(x, y);
        }
    }
}
