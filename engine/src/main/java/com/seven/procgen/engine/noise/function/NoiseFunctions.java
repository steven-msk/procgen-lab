package com.seven.procgen.engine.noise.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.seven.procgen.engine.noise.combiner.NoiseCombiner;
import com.seven.procgen.engine.noise.combiner.NoiseCombinerProvider;
import com.seven.procgen.engine.noise.sampler.NoiseSamplerProvider;
import com.seven.procgen.engine.registry.Identifier;
import com.seven.procgen.engine.util.serialization.Codecs;
import org.jspecify.annotations.NonNull;

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

public final class NoiseFunctions {

    public static NoiseFunction createSampler(String id, NoiseSamplerProvider provider) {
        return createSampler(id, provider, 1.0d, 1.0d, 1.0d);
    }

    public static NoiseFunction createSampler(String id, NoiseSamplerProvider provider, double xScale, double yScale, double zScale) {
        return new Sampler(new NoiseFunction.SamplerEntry(Identifier.of(id), provider, null), xScale, yScale, zScale);
    }

    public static <T extends NoiseCombiner.Parameters> NoiseFunction createCombiner(
        String id, NoiseCombinerProvider<T> provider, NoiseSamplerProvider samplerProvider, T parameters
    ) {
        return createCombiner(id, provider, samplerProvider, parameters, 1.0d, 1.0d, 1.0d);
    }

    public static <T extends NoiseCombiner.Parameters> NoiseFunction createCombiner(
        String id, NoiseCombinerProvider<T> provider, NoiseSamplerProvider samplerProvider, T parameters, double xScale, double yScale, double zScale
    ) {
        return new Combiner<>(new NoiseFunction.CombinerEntry<>(Identifier.of(id), provider, samplerProvider, parameters, null), xScale, yScale, zScale);
    }

    public static NoiseFunction constant(double value) {
        return new Constant(value);
    }

    public static NoiseFunction abs(NoiseFunction f) {
        return new UnaryFunction(UnaryOperation.ABS, f);
    }

    public static NoiseFunction negate(NoiseFunction f) {
        return new UnaryFunction(UnaryOperation.NEGATE, f);
    }

    public static NoiseFunction square(NoiseFunction f) {
        return new UnaryFunction(UnaryOperation.SQUARE, f);
    }

    public static NoiseFunction cube(NoiseFunction f) {
        return new UnaryFunction(UnaryOperation.CUBE, f);
    }

    public static NoiseFunction oneMinus(NoiseFunction f) {
        return new UnaryFunction(UnaryOperation.ONE_MINUS, f);
    }

    public static NoiseFunction add(NoiseFunction a, NoiseFunction b) {
        return new BinaryFunction(BinaryOperation.ADD, a, b);
    }

    public static NoiseFunction subtract(NoiseFunction a, NoiseFunction b) {
        return new BinaryFunction(BinaryOperation.SUBTRACT, a, b);
    }

    public static NoiseFunction mul(NoiseFunction a, NoiseFunction b) {
        return new BinaryFunction(BinaryOperation.MULTIPLY, a, b);
    }

    public static NoiseFunction min(NoiseFunction a, NoiseFunction b) {
        return new BinaryFunction(BinaryOperation.MIN, a, b);
    }

    public static NoiseFunction max(NoiseFunction a, NoiseFunction b) {
        return new BinaryFunction(BinaryOperation.MAX, a, b);
    }

    public static NoiseFunction affine(NoiseFunction f, double scale, double bias) {
        return new Affine(f, scale, bias);
    }

    public static NoiseFunction remap01(NoiseFunction f) {
        return new Affine(f, 0.5d, 0.5d);
    }

    public static NoiseFunction clamp(NoiseFunction f, double min, double max) {
        return new Clamp(f, min, max);
    }

    public static NoiseFunction lerp(NoiseFunction a, NoiseFunction b, NoiseFunction t) {
        return new Lerp(a, b, t);
    }

    public record Sampler(NoiseFunction.SamplerEntry samplerEntry, double xScale, double yScale, double zScale) implements NoiseFunction {
        public static final MapCodec<Sampler> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Identifier.CODEC.fieldOf("id").forGetter(n -> n.samplerEntry().id()),
            NoiseSamplerProvider.CODEC.fieldOf("sampler").forGetter(n -> n.samplerEntry().provider()),
            Codec.DOUBLE.fieldOf("scale_x").forGetter(Sampler::xScale),
            Codec.DOUBLE.fieldOf("scale_y").forGetter(Sampler::yScale),
            Codec.DOUBLE.fieldOf("scale_z").forGetter(Sampler::zScale)
        ).apply(i, (id, provider, sx, sy, sz) -> new Sampler(new SamplerEntry(id, provider, null), sx, sy, sz)));

        @Override
        public double compute(Context context) {
            return samplerEntry.get(context.x() * xScale, context.y() * yScale, context.z() * zScale);
        }

        @Override
        public NoiseFunction mapAll(Visitor visitor) {
            return visitor.apply(new Sampler(visitor.visitSampler(samplerEntry), xScale, yScale, zScale));
        }

        @Override
        public MapCodec<? extends NoiseFunction> codec() {
            return CODEC;
        }

        @Override
        public @NonNull String toString() {
            return "Sampler[\"" + samplerEntry + "\", xScale=" + xScale + ", yscale=" + yScale + "]";
        }
    }

    public record Combiner<T extends NoiseCombiner.Parameters>(NoiseFunction.CombinerEntry<T> combinerEntry, double xScale, double yScale, double zScale) implements NoiseFunction {
        public static final MapCodec<Combiner<? extends NoiseCombiner.Parameters>> CODEC =
            RecordCodecBuilder.mapCodec(i -> i.group(
                NoiseCombinerProvider.CODEC.<CombinerEntry<?>>dispatchMap("type", CombinerEntry::provider, Combiner::entryCodec).fieldOf("combiner").forGetter(Combiner::combinerEntry),
                Codec.DOUBLE.fieldOf("scale_x").forGetter(Combiner::xScale),
                Codec.DOUBLE.fieldOf("scale_y").forGetter(Combiner::yScale),
                Codec.DOUBLE.fieldOf("scale_z").forGetter(Combiner::zScale)
            ).apply(i, Combiner::new));

        private static <T extends NoiseCombiner.Parameters> MapCodec<CombinerEntry<T>> entryCodec(NoiseCombinerProvider<T> provider) {
            return RecordCodecBuilder.mapCodec(i -> i.group(
                Identifier.CODEC.fieldOf("id").forGetter(CombinerEntry::id),
                NoiseSamplerProvider.CODEC.fieldOf("sampler").forGetter(CombinerEntry::samplerProvider),
                provider.parametersCodec().fieldOf("parameters").forGetter(CombinerEntry::parameters)
            ).apply(i, (id, sampler, params) -> new CombinerEntry<>(id, provider, sampler, params, null)));
        }

        @Override
        public double compute(Context context) {
            return combinerEntry.combine(context.x() * xScale, context.y() * yScale, context.z() * zScale);
        }

        @Override
        public NoiseFunction mapAll(Visitor visitor) {
            return visitor.apply(new Combiner<>(visitor.visitCombiner(combinerEntry), xScale, yScale, zScale));
        }

        @Override
        public MapCodec<? extends NoiseFunction> codec() {
            return CODEC;
        }

        @Override
        public @NonNull String toString() {
            return "Combiner[\"" + combinerEntry + "\", xScale=" + xScale + ", yScale=" + yScale + "]";
        }
    }

    public record Constant(double value) implements NoiseFunction {
        public static final MapCodec<Constant> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.fieldOf("value").forGetter(Constant::value)
        ).apply(i, Constant::new));

        @Override
        public double compute(Context context) {
            return value;
        }

        @Override
        public NoiseFunction mapAll(Visitor visitor) {
            return visitor.apply(this);
        }


        @Override
        public MapCodec<? extends NoiseFunction> codec() {
            return CODEC;
        }

        @Override
        public @NonNull String toString() {
            return "Constant[" + value + "]";
        }
    }

    public record UnaryFunction(UnaryOperation operation, NoiseFunction input) implements NoiseFunction {
        public static final MapCodec<UnaryFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            UnaryOperation.CODEC.fieldOf("operation").forGetter(UnaryFunction::operation),
            NoiseFunction.CODEC.fieldOf("input").forGetter(UnaryFunction::input)
        ).apply(i, UnaryFunction::new));

        @Override
        public double compute(Context context) {
            return operation.apply(input.compute(context));
        }

        @Override
        public NoiseFunction mapAll(Visitor visitor) {
            return visitor.apply(new UnaryFunction(operation, input.mapAll(visitor)));
        }

        @Override
        public MapCodec<? extends NoiseFunction> codec() {
            return CODEC;
        }

        @Override
        public @NonNull String toString() {
            return operation + "[" + input + "]";
        }
    }

    public record BinaryFunction(BinaryOperation operation, NoiseFunction a, NoiseFunction b) implements NoiseFunction {
        public static final MapCodec<BinaryFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BinaryOperation.CODEC.fieldOf("operation").forGetter(BinaryFunction::operation),
            NoiseFunction.CODEC.fieldOf("a").forGetter(BinaryFunction::a),
            NoiseFunction.CODEC.fieldOf("b").forGetter(BinaryFunction::b)
        ).apply(i, BinaryFunction::new));

        @Override
        public double compute(Context context) {
            return operation.apply(a.compute(context), b.compute(context));
        }

        @Override
        public NoiseFunction mapAll(Visitor visitor) {
            return visitor.apply(new BinaryFunction(operation, a.mapAll(visitor), b.mapAll(visitor)));
        }

        @Override
        public MapCodec<? extends NoiseFunction> codec() {
            return CODEC;
        }

        @Override
        public @NonNull String toString() {
            return operation + "[" + a + ", " + b + "]";
        }
    }

    public record Clamp(NoiseFunction input, double min, double max) implements NoiseFunction {
        public static final MapCodec<Clamp> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            NoiseFunction.CODEC.fieldOf("input").forGetter(Clamp::input),
            Codec.DOUBLE.fieldOf("min").forGetter(Clamp::min),
            Codec.DOUBLE.fieldOf("max").forGetter(Clamp::max)
        ).apply(i, Clamp::new));

        public Clamp {
            if (min > max) throw new IllegalArgumentException("min > max: " + min + " > " + max);
        }

        @Override
        public double compute(Context context) {
            return Math.clamp(input.compute(context), min, max);
        }

        @Override
        public NoiseFunction mapAll(Visitor visitor) {
            return visitor.apply(new Clamp(input.mapAll(visitor), min, max));
        }

        @Override
        public MapCodec<? extends NoiseFunction> codec() {
            return CODEC;
        }

        @Override
        public @NonNull String toString() {
            return "Clamp[" + min + ".." + max + ": " + input + "]";
        }
    }

    /// Not clamped, t is expected in [0, 1]
    public record Lerp(NoiseFunction from, NoiseFunction to, NoiseFunction t) implements NoiseFunction {
        public static final MapCodec<Lerp> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            NoiseFunction.CODEC.fieldOf("from").forGetter(Lerp::from),
            NoiseFunction.CODEC.fieldOf("to").forGetter(Lerp::to),
            NoiseFunction.CODEC.fieldOf("t").forGetter(Lerp::t)
        ).apply(i, Lerp::new));

        @Override
        public double compute(Context context) {
            double av = from.compute(context);
            return av + (to.compute(context) - av) * t.compute(context);
        }

        @Override
        public NoiseFunction mapAll(Visitor visitor) {
            return visitor.apply(new Lerp(from.mapAll(visitor), to.mapAll(visitor), t.mapAll(visitor)));
        }

        @Override
        public MapCodec<? extends NoiseFunction> codec() {
            return CODEC;
        }

        @Override
        public @NonNull String toString() {
            return "Lerp[" + from + " -> " + to + " : " + t + "]";
        }
    }

    /// input * scale + bias. Handy for remapping ranges, e.g. [-1, 1] -> [0, 1] is scale 0.5, bias 0.5.
    public record Affine(NoiseFunction input, double scale, double bias) implements NoiseFunction {
        public static final MapCodec<Affine> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            NoiseFunction.CODEC.fieldOf("input").forGetter(Affine::input),
            Codec.DOUBLE.fieldOf("scale").forGetter(Affine::scale),
            Codec.DOUBLE.fieldOf("bias").forGetter(Affine::bias)
        ).apply(i, Affine::new));

        @Override
        public double compute(Context context) {
            return input.compute(context) * scale + bias;
        }

        @Override
        public NoiseFunction mapAll(Visitor visitor) {
            return visitor.apply(new Affine(input.mapAll(visitor), scale, bias));
        }

        @Override
        public MapCodec<? extends NoiseFunction> codec() {
            return CODEC;
        }

        @Override
        public @NonNull String toString() {
            return "Affine[" + input + " * " + scale + " + " + bias + "]";
        }
    }

    public enum UnaryOperation {
        ABS(Math::abs),
        NEGATE(v -> -v),
        SQUARE(v -> v * v),
        CUBE(v -> v * v * v),
        ONE_MINUS(v -> 1.0d - v);

        public static final Codec<UnaryOperation> CODEC = Codecs.enumOf(UnaryOperation.class);

        private final DoubleUnaryOperator operator;

        UnaryOperation(DoubleUnaryOperator operator) {
            this.operator = operator;
        }

        public double apply(double v) {
            return operator.applyAsDouble(v);
        }
    }

    public enum BinaryOperation {
        ADD(Double::sum),
        SUBTRACT((a, b) -> a - b),
        MULTIPLY((a, b) -> a * b),
        MIN(Math::min),
        MAX(Math::max);

        public static final Codec<BinaryOperation> CODEC = Codecs.enumOf(BinaryOperation.class);

        private final DoubleBinaryOperator operator;

        BinaryOperation(DoubleBinaryOperator operator) {
            this.operator = operator;
        }

        public double apply(double a, double b) {
            return operator.applyAsDouble(a, b);
        }
    }
}
