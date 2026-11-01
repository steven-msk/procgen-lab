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
import com.seven.procgen.engine.registry.*;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public interface NoiseFunction {
    Codec<NoiseFunction> CODEC = Registry.NOISE_FUNCTION.byNameCodec().dispatch(NoiseFunction::codec, Function.identity());

    double compute(Context context);

    MapCodec<? extends NoiseFunction> codec();

    NoiseFunction mapAll(Visitor visitor);

    /// Non-recursive; returns 'this' if no child changed
    NoiseFunction mapChildren(UnaryOperator<NoiseFunction> function);

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

        default <T> SamplerEntry<T> visitSampler(SamplerEntry<T> sampler) {
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

    record SamplerEntry<T>(Identifier id, RegistryKey<NoiseSamplerProvider<?>> providerKey, T parameters, @Nullable NoiseSampler sampler) {
        public static <T> SamplerEntry<T> of(Identifier id, T parameters, RegistryKey<NoiseSamplerProvider<?>> provider) {
            return new SamplerEntry<>(id, provider, parameters, null);
        }

        public double get(double x, double y, double z) {
            if (sampler == null) throw new IllegalStateException("Sampler not bound: " + id);
            return sampler.sample(x, y, z);
        }

        @SuppressWarnings("unchecked")
        public SamplerEntry<T> bind(RandomFork fork, RegistryEntryLookup.RegistryLookup registryLookup) {
            NoiseSamplerProvider<T> provider = (NoiseSamplerProvider<T>)registryLookup.getOrThrow(Registries.NOISE_SAMPLER_PROVIDER).getOrThrow(providerKey).value();
            RandomSource random = fork.fromHashOf(id.toString());
            return new SamplerEntry<>(id, providerKey, parameters, provider.getSampler(random.nextLong()));
        }

        @Override
        public @NonNull String toString() {
            return id.toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, providerKey);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof SamplerEntry<?> e && id.equals(e.id) && providerKey.equals(e.providerKey);
        }
    }

    record CombinerEntry<T extends NoiseCombiner.Parameters>(
        Identifier id, RegistryKey<NoiseCombinerProvider<? extends NoiseCombiner.Parameters>> combinerKey,
        RegistryKey<NoiseSamplerProvider<?>> samplerKey, T parameters, @Nullable NoiseCombiner<T> combiner
    ) {
        public double combine(double x, double y, double z) {
            if (combiner == null) throw new IllegalStateException("Combiner not bound: " + id);
            return combiner.compute(x, y, z);
        }

        @SuppressWarnings("unchecked")
        public CombinerEntry<T> bind(RandomFork fork, RegistryEntryLookup.RegistryLookup registryLookup) {
            NoiseSamplerProvider<?> sampler = registryLookup.getOrThrow(Registries.NOISE_SAMPLER_PROVIDER).getOrThrow(samplerKey).value();
            NoiseCombinerProvider<T> combiner = (NoiseCombinerProvider<T>)registryLookup.getOrThrow(Registries.NOISE_COMBINER_PROVIDER).getOrThrow(combinerKey).value();
            RandomSource random = fork.fromHashOf(id.toString());
            return new CombinerEntry<>(id, combinerKey, samplerKey, parameters, combiner.getCombiner(random, sampler, parameters));
        }

        @Override
        public @NonNull String toString() {
            return id.toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, combinerKey, samplerKey, parameters);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof CombinerEntry<?> other
                && id.equals(other.id)
                && combinerKey.equals(other.combinerKey)
                && samplerKey.equals(other.samplerKey)
                && parameters.equals(other.parameters);
        }
    }
}
