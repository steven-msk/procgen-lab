package com.seven.procgen.engine.noise.function;

import com.seven.procgen.engine.noise.combiner.NoiseCombiner;
import com.seven.procgen.engine.noise.combiner.NoiseCombinerProvider;
import com.seven.procgen.engine.noise.sampler.NoiseSamplerProvider;
import com.seven.procgen.engine.registry.Identifier;

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

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
        return new Combiner<>(new NoiseFunction.CombinerEntry<>(Identifier.of(id), provider, samplerProvider, parameters, null), xScale, yScale);
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

    public static NoiseFunction mult(NoiseFunction a, NoiseFunction b) {
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
            return visitor.apply(new Combiner<>(visitor.visitCombiner(this.combinerEntry), xScale, yScale));
        }
    }

    public record Constant(double value) implements NoiseFunction {
        @Override
        public double compute(Context context) {
            return value;
        }

        @Override
        public NoiseFunction mapAll(Visitor visitor) {
            return visitor.apply(this);
        }
    }

    public record UnaryFunction(UnaryOperation operation, NoiseFunction input) implements NoiseFunction {
        @Override
        public double compute(Context context) {
            return operation.apply(input.compute(context));
        }

        @Override
        public NoiseFunction mapAll(Visitor visitor) {
            return visitor.apply(new UnaryFunction(operation, input.mapAll(visitor)));
        }
    }

    public record BinaryFunction(BinaryOperation operation, NoiseFunction a, NoiseFunction b) implements NoiseFunction {
        @Override
        public double compute(Context context) {
            return operation.apply(a.compute(context), b.compute(context));
        }

        @Override
        public NoiseFunction mapAll(Visitor visitor) {
            return visitor.apply(new BinaryFunction(operation, a.mapAll(visitor), b.mapAll(visitor)));
        }
    }

    public record Clamp(NoiseFunction input, double min, double max) implements NoiseFunction {
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
    }

    /// Not clamped, t is expected in [0, 1]
    public record Lerp(NoiseFunction a, NoiseFunction b, NoiseFunction t) implements NoiseFunction {
        @Override
        public double compute(Context context) {
            double av = a.compute(context);
            return av + (b.compute(context) - av) * t.compute(context);
        }

        @Override
        public NoiseFunction mapAll(Visitor visitor) {
            return visitor.apply(new Lerp(a.mapAll(visitor), b.mapAll(visitor), t.mapAll(visitor)));
        }
    }

    /// input * scale + bias. Handy for remapping ranges, e.g. [-1, 1] -> [0, 1] is scale 0.5, bias 0.5.
    public record Affine(NoiseFunction input, double scale, double bias) implements NoiseFunction {
        @Override
        public double compute(Context context) {
            return input.compute(context) * scale + bias;
        }

        @Override
        public NoiseFunction mapAll(Visitor visitor) {
            return visitor.apply(new Affine(input.mapAll(visitor), scale, bias));
        }
    }

    public enum UnaryOperation {
        ABS(Math::abs),
        NEGATE(v -> -v),
        SQUARE(v -> v * v),
        CUBE(v -> v * v * v),
        ONE_MINUS(v -> 1.0d - v);

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

        private final DoubleBinaryOperator operator;

        BinaryOperation(DoubleBinaryOperator operator) {
            this.operator = operator;
        }

        public double apply(double a, double b) {
            return operator.applyAsDouble(a, b);
        }
    }
}
