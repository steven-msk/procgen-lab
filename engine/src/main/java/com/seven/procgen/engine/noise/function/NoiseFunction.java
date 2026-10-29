package com.seven.procgen.engine.noise.function;

import com.seven.procgen.engine.noise.combiner.NoiseCombiner;
import com.seven.procgen.engine.noise.combiner.NoiseCombinerProvider;
import com.seven.procgen.engine.noise.sampler.NoiseSampler;
import com.seven.procgen.engine.noise.sampler.NoiseSamplerProvider;
import com.seven.procgen.engine.random.RandomFork;
import com.seven.procgen.engine.random.RandomSource;
import com.seven.procgen.engine.registry.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public interface NoiseFunction {
    double compute(Context context);

    NoiseFunction mapAll(Visitor visitor);

    default NoiseFunction abs() {
        return NoiseFunctions.abs(this);
    }

    default NoiseFunction add(NoiseFunction other) {
        return NoiseFunctions.add(this, other);
    }

    default NoiseFunction mul(NoiseFunction other) {
        return NoiseFunctions.mul(this, other);
    }

    default NoiseFunction mul(double v) {
        return NoiseFunctions.mul(this, NoiseFunctions.constant(v));
    }

    default NoiseFunction clamp(double min, double max) {
        return NoiseFunctions.clamp(this, min, max);
    }

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

        double z();
    }

    record SinglePointContext(double x, double y, double z) implements Context {
    }

    record SamplerEntry(Identifier id, NoiseSamplerProvider provider, @Nullable NoiseSampler sampler) {
        public double get(double x, double y, double z) {
            if (sampler == null) throw new IllegalStateException("Compute called with null sampler");
            return sampler.sample(x, y, z);
        }

        public SamplerEntry bind(RandomFork fork) {
            RandomSource random = fork.fromHashOf(id.toString());
            return new SamplerEntry(id, provider, provider.getSampler(random.nextLong()));
        }

        @Override
        @NonNull
        public String toString() {
            return id.toString();
        }
    }

    record CombinerEntry<T extends NoiseCombiner.Parameters>(Identifier id, NoiseCombinerProvider<T> provider, NoiseSamplerProvider samplerProvider, T parameters, @Nullable NoiseCombiner<T> combiner) {
        public double combine(double x, double y, double z) {
            if (combiner == null) throw new IllegalStateException("Combine called with null combiner");
            return combiner.compute(x, y, z);
        }

        public CombinerEntry<T> bind(RandomFork fork) {
            RandomSource random = fork.fromHashOf(id.toString());
            return new CombinerEntry<>(id, provider, samplerProvider, parameters, provider.getCombiner(random, samplerProvider, parameters));
        }

        @Override
        @NonNull
        public String toString() {
            return id.toString();
        }
    }
}
