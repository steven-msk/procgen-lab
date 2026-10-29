package com.seven.procgen.engine.noise.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.seven.procgen.engine.noise.combiner.NoiseCombiner;
import com.seven.procgen.engine.noise.combiner.NoiseCombinerProvider;
import com.seven.procgen.engine.noise.sampler.NoiseSampler;
import com.seven.procgen.engine.noise.sampler.NoiseSamplerProvider;
import com.seven.procgen.engine.random.RandomFork;
import com.seven.procgen.engine.random.RandomSource;
import com.seven.procgen.engine.registry.Identifier;
import com.seven.procgen.engine.registry.Registry;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;

public interface NoiseFunction {
    Codec<NoiseFunction> CODEC = Registry.NOISE_FUNCTION.byNameCodec().dispatch(NoiseFunction::codec, Function.identity());

    double compute(Context context);

    MapCodec<? extends NoiseFunction> codec();

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

    static <T extends NoiseFunction> MapCodec<T> unaryCodec(Function<NoiseFunction, T> ctor, Function<T, NoiseFunction> in) {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            NoiseFunction.CODEC.fieldOf("input").forGetter(in)
        ).apply(i, ctor));
    }

    static <T extends NoiseFunction> MapCodec<T> binaryCodec(
        BiFunction<NoiseFunction, NoiseFunction, T> ctor,
        Function<T, NoiseFunction> a, Function<T, NoiseFunction> b
    ) {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            NoiseFunction.CODEC.fieldOf("a").forGetter(a),
            NoiseFunction.CODEC.fieldOf("b").forGetter(b)
        ).apply(i, ctor));
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
        public static SamplerEntry of(Identifier id, NoiseSamplerProvider provider) {
            return new SamplerEntry(id, provider, null);
        }

        public double get(double x, double y, double z) {
            if (sampler == null) throw new IllegalStateException("Compute called with null sampler");
            return sampler.sample(x, y, z);
        }

        public SamplerEntry bind(RandomFork fork) {
            RandomSource random = fork.fromHashOf(id.toString());
            return new SamplerEntry(id, provider, provider.getSampler(random.nextLong()));
        }

        @Override
        public @NonNull String toString() {
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
