package com.seven.procgen.engine.noise.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.seven.procgen.engine.noise.combiner.NoiseCombiner;
import com.seven.procgen.engine.noise.combiner.NoiseCombinerProvider;
import com.seven.procgen.engine.noise.sampler.NoiseSamplerProvider;
import com.seven.procgen.engine.registry.Identifier;
import com.seven.procgen.engine.registry.Registry;
import com.seven.procgen.engine.registry.RegistryKey;
import com.seven.procgen.engine.util.math.Axis;
import com.seven.procgen.engine.util.math.Maths;
import com.seven.procgen.engine.util.serialization.Codecs;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.UnaryOperator;

public final class NoiseFunctions {

    public static NoiseFunction createSampler(String id, RegistryKey<NoiseSamplerProvider> provider) {
        return createSampler(id, provider, 1.0d, 1.0d, 1.0d);
    }

    public static NoiseFunction createSampler(String id, RegistryKey<NoiseSamplerProvider> provider, double xScale, double yScale, double zScale) {
        return new Sampler(new NoiseFunction.SamplerEntry(Identifier.of(id), provider, null), xScale, yScale, zScale);
    }

    public static <T extends NoiseCombiner.Parameters> NoiseFunction createCombiner(
        String id, RegistryKey<NoiseCombinerProvider<? extends NoiseCombiner.Parameters>> combinerKey, RegistryKey<NoiseSamplerProvider> samplerKey, T parameters
    ) {
        return createCombiner(id, combinerKey, samplerKey, parameters, 1.0d, 1.0d, 1.0d);
    }

    public static <T extends NoiseCombiner.Parameters> NoiseFunction createCombiner(
        String id, RegistryKey<NoiseCombinerProvider<? extends NoiseCombiner.Parameters>> combinerKey, RegistryKey<NoiseSamplerProvider> samplerKey, T parameters, double xScale, double yScale, double zScale
    ) {
        return new Combiner<>(new NoiseFunction.CombinerEntry<>(Identifier.of(id), combinerKey, samplerKey, parameters, null), xScale, yScale, zScale);
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

    public static NoiseFunction scale(NoiseFunction f, double xScale, double yScale, double zScale) {
        return new Scale(f, xScale, yScale, zScale);
    }

    public static NoiseFunction scale(NoiseFunction f, double factor) {
        return Scale.uniform(f, factor);
    }

    public static NoiseFunction offset(NoiseFunction f, double offsetX, double offsetY, double offsetZ) {
        return new Offset(f, offsetX, offsetY, offsetZ);
    }

    public static NoiseFunction rotate(NoiseFunction f, double degrees, double pivotX, double pivotY) {
        return new Rotate(f, degrees, pivotX, pivotY);
    }

    public static NoiseFunction warp(NoiseFunction f, NoiseFunction offsetX, NoiseFunction offsetY, double amplitude) {
        return new DomainWarp(f, offsetX, offsetY, amplitude);
    }

    public record Sampler(NoiseFunction.SamplerEntry samplerEntry, double xScale, double yScale, double zScale) implements NoiseFunction {
        public static final MapCodec<Sampler> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Identifier.CODEC.fieldOf("id").forGetter(n -> n.samplerEntry().id()),
            NoiseSamplerProvider.KEY_CODEC.fieldOf("sampler").forGetter(n -> n.samplerEntry().providerKey()),
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
        public NoiseFunction mapChildren(UnaryOperator<NoiseFunction> function) {
            return this;
        }

        @Override
        public MapCodec<? extends NoiseFunction> codec() {
            return CODEC;
        }

        @Override
        public @NonNull String toString() {
            return "Sampler[\"" + samplerEntry + "\", xScale=" + xScale + ", yScale=" + yScale + ", zScale=" + zScale + "]";
        }
    }

    public record Combiner<T extends NoiseCombiner.Parameters>(NoiseFunction.CombinerEntry<T> combinerEntry, double xScale, double yScale, double zScale) implements NoiseFunction {
        public static final MapCodec<Combiner<? extends NoiseCombiner.Parameters>> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            NoiseCombinerProvider.KEY_CODEC.<CombinerEntry<?>>dispatchMap("type", CombinerEntry::combinerKey, Combiner::entryCodec)
                .fieldOf("combiner").forGetter(Combiner::combinerEntry),
            Codec.DOUBLE.fieldOf("scale_x").forGetter(Combiner::xScale),
            Codec.DOUBLE.fieldOf("scale_y").forGetter(Combiner::yScale),
            Codec.DOUBLE.fieldOf("scale_z").forGetter(Combiner::zScale)
        ).apply(i, Combiner::new));

        @SuppressWarnings("unchecked")
        private static <T extends NoiseCombiner.Parameters> MapCodec<CombinerEntry<T>> entryCodec(RegistryKey<NoiseCombinerProvider<? extends NoiseCombiner.Parameters>> provider) {
            Codec<T> parametersCodec = (Codec<T>)Registry.NOISE_COMBINER_PROVIDER.getOrThrow(provider).value().parametersCodec();
            return RecordCodecBuilder.mapCodec(i -> i.group(
                Identifier.CODEC.fieldOf("id").forGetter(CombinerEntry::id),
                NoiseSamplerProvider.KEY_CODEC.fieldOf("sampler").forGetter(CombinerEntry::samplerKey),
                parametersCodec.fieldOf("parameters").forGetter(CombinerEntry::parameters)
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
        public NoiseFunction mapChildren(UnaryOperator<NoiseFunction> function) {
            return this;
        }

        @Override
        public MapCodec<? extends NoiseFunction> codec() {
            return CODEC;
        }

        @Override
        public @NonNull String toString() {
            return "Combiner[\"" + combinerEntry + "\", xScale=" + xScale + ", yScale=" + yScale + ", zScale=" + zScale + "]";
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
        public NoiseFunction mapChildren(UnaryOperator<NoiseFunction> function) {
            return this;
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
        public NoiseFunction mapChildren(UnaryOperator<NoiseFunction> function) {
            NoiseFunction newInput = function.apply(input);
            return newInput == input ? this : new UnaryFunction(operation, newInput);
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
        public NoiseFunction mapChildren(UnaryOperator<NoiseFunction> function) {
            NoiseFunction newA = function.apply(a);
            NoiseFunction newB = function.apply(b);
            return newA == a && newB == b ? this : new BinaryFunction(operation, newA, newB);
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
        public NoiseFunction mapChildren(UnaryOperator<NoiseFunction> function) {
            NoiseFunction newInput = function.apply(input);
            return newInput == input ? this : new Clamp(newInput, min, max);
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
        public NoiseFunction mapChildren(UnaryOperator<NoiseFunction> function) {
            NoiseFunction newFrom = function.apply(from);
            NoiseFunction newTo = function.apply(to);
            NoiseFunction newT = function.apply(t);
            return newFrom == from && newTo == to && newT == t ? this : new Lerp(newFrom, newTo, newT);
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
        public NoiseFunction mapChildren(UnaryOperator<NoiseFunction> function) {
            NoiseFunction newInput = function.apply(input);
            return newInput == input ? this : new Affine(newInput, scale, bias);
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

    public record Reference(Identifier id) implements NoiseFunction {
        public static final MapCodec<Reference> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Identifier.CODEC.fieldOf("id").forGetter(Reference::id)
        ).apply(i, Reference::new));

        @Override
        public double compute(Context context) {
            throw new IllegalStateException("Unresolved reference '" + id + "'");
        }

        @Override
        public MapCodec<? extends NoiseFunction> codec() {
            return CODEC;
        }

        @Override
        public NoiseFunction mapAll(Visitor visitor) {
            throw new IllegalStateException("Unresolved reference '" + id + "'");
        }

        @Override
        public NoiseFunction mapChildren(UnaryOperator<NoiseFunction> function) {
            return this;
        }
    }

    // Multiplies the sampling coordinates per axis. Basically a frequency multiplier, e.g 2 = features half as large
    public record Scale(NoiseFunction input, double x, double y, double z) implements NoiseFunction {
        public static final MapCodec<Scale> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            NoiseFunction.CODEC.fieldOf("input").forGetter(Scale::input),
            Codec.DOUBLE.fieldOf("x").forGetter(Scale::x),
            Codec.DOUBLE.fieldOf("y").forGetter(Scale::y),
            Codec.DOUBLE.optionalFieldOf("z", 1.0d).forGetter(Scale::z)
        ).apply(i, Scale::new));

        public static Scale uniform(NoiseFunction input, double factor) {
            return new Scale(input, factor, factor, factor);
        }

        @Override
        public double compute(Context context) {
            return input.compute(new SinglePointContext(context.x() * x, context.y() * y, context.z() * z));
        }

        @Override
        public MapCodec<? extends NoiseFunction> codec() {
            return CODEC;
        }

        @Override
        public NoiseFunction mapAll(Visitor visitor) {
            return visitor.apply(new Scale(input.mapAll(visitor), x, y, z));
        }

        @Override
        public NoiseFunction mapChildren(UnaryOperator<NoiseFunction> function) {
            NoiseFunction n0 = function.apply(input);
            return n0 == input ? this : new Scale(n0, x, y, z);
        }
    }

    /// Adds a constant to the sampling coordinates
    public record Offset(NoiseFunction input, double dx, double dy, double dz) implements NoiseFunction {
        public static final MapCodec<Offset> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            NoiseFunction.CODEC.fieldOf("input").forGetter(Offset::input),
            Codec.DOUBLE.optionalFieldOf("dx", 0.0d).forGetter(Offset::dx),
            Codec.DOUBLE.optionalFieldOf("dy", 0.0d).forGetter(Offset::dy),
            Codec.DOUBLE.optionalFieldOf("dz", 0.0d).forGetter(Offset::dz)
        ).apply(i, Offset::new));

        @Override
        public double compute(Context context) {
            return input.compute(new SinglePointContext(context.x() + dx, context.y() + dy, context.z() + dz));
        }

        @Override
        public MapCodec<? extends NoiseFunction> codec() {
            return CODEC;
        }

        @Override
        public NoiseFunction mapAll(Visitor visitor) {
            return visitor.apply(new Offset(input.mapAll(visitor), dx, dy, dz));
        }

        @Override
        public NoiseFunction mapChildren(UnaryOperator<NoiseFunction> function) {
            NoiseFunction n0 = function.apply(input);
            return n0 == input ? this : new Offset(n0, dx, dy, dz);
        }
    }

    public record Rotate(NoiseFunction input, double degrees, double pivotX, double pivotY, double cos, double sin) implements NoiseFunction {
        public static final MapCodec<Rotate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            NoiseFunction.CODEC.fieldOf("input").forGetter(Rotate::input),
            Codec.DOUBLE.fieldOf("degrees").forGetter(Rotate::degrees),
            Codec.DOUBLE.optionalFieldOf("pivot_x", 0.0d).forGetter(Rotate::pivotX),
            Codec.DOUBLE.optionalFieldOf("pivot_y", 0.0d).forGetter(Rotate::pivotY)
        ).apply(i, Rotate::new));

        public Rotate {
            double radians = Math.toRadians(degrees);
            cos = Math.cos(radians);
            sin = Math.sin(radians);
        }

        public Rotate(NoiseFunction input, double degrees, double pivotX, double pivotY) {
            this(input, degrees, pivotX, pivotY, 0.0d, 0.0d); // cos/sin are overwritten by compact ctor
        }

        @Override
        public double compute(Context context) {
            double dx = context.x() - pivotX;
            double dy = context.y() - pivotY;
            return input.compute(new SinglePointContext(
                pivotX + dx * cos - dy * sin,
                pivotY + dx * sin + dy * cos,
                context.z()
            ));
        }

        @Override
        public MapCodec<? extends NoiseFunction> codec() {
            return CODEC;
        }

        @Override
        public NoiseFunction mapAll(Visitor visitor) {
            return visitor.apply(new Rotate(input.mapAll(visitor), degrees, pivotX, pivotY));
        }

        @Override
        public NoiseFunction mapChildren(UnaryOperator<NoiseFunction> function) {
            NoiseFunction n0 = function.apply(input);
            return n0 == input ? this : new Rotate(n0, degrees, pivotX, pivotY);
        }
    }

    /// Samples input at (x + offsetX * amplitude, y + offsetY * amplitude, z).
    /// offsetX / offsetY are evaluated at the INCOMING context, so they stay in the grid pass and keep their
    /// cheap previews. Only `input` is evaluated at the displaced point.
    /// Wire offsetX and offsetY to differently seeded sources; the same node on both gives a purely diagonal warp.
    /// amplitude is in coordinate units (after any Scale you put in front of the offset noises).
    public record DomainWarp(NoiseFunction input, NoiseFunction offsetX, NoiseFunction offsetY, double amplitude) implements NoiseFunction {
        public static final MapCodec<DomainWarp> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            NoiseFunction.CODEC.fieldOf("input").forGetter(DomainWarp::input),
            NoiseFunction.CODEC.fieldOf("offset_x").forGetter(DomainWarp::offsetX),
            NoiseFunction.CODEC.fieldOf("offset_y").forGetter(DomainWarp::offsetY),
            Codec.DOUBLE.fieldOf("amplitude").forGetter(DomainWarp::amplitude)
        ).apply(i, DomainWarp::new));

        @Override
        public double compute(Context context) {
            double dx = offsetX.compute(context) * amplitude;
            double dy = offsetY.compute(context) * amplitude;
            return input.compute(new SinglePointContext(context.x() + dx, context.y() + dy, context.z()));
        }

        @Override
        public MapCodec<? extends NoiseFunction> codec() {
            return CODEC;
        }

        @Override
        public NoiseFunction mapAll(Visitor visitor) {
            return visitor.apply(new DomainWarp(input.mapAll(visitor), offsetX.mapAll(visitor), offsetY.mapAll(visitor), amplitude));
        }

        @Override
        public NoiseFunction mapChildren(UnaryOperator<NoiseFunction> function) {
            NoiseFunction n0 = function.apply(input);
            NoiseFunction n1 = function.apply(offsetX);
            NoiseFunction n2 = function.apply(offsetY);
            return n0 == input && n1 == offsetX && n2 == offsetY ? this : new DomainWarp(n0, n1, n2, amplitude);

        }
    }

    /// Clamped linear ramp along one axis: fromValue at `from`, toValue at `to`, held flat outside.
    /// Defaults to the usual noise range (-1..1). from > to gives a descending ramp.
    public record Gradient(Axis axis, double from, double to, double fromValue, double toValue) implements NoiseFunction {
        public static final MapCodec<Gradient> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Axis.CODEC.fieldOf("axis").forGetter(Gradient::axis),
            Codec.DOUBLE.fieldOf("from").forGetter(Gradient::from),
            Codec.DOUBLE.fieldOf("to").forGetter(Gradient::to),
            Codec.DOUBLE.optionalFieldOf("from_value", -1.0d).forGetter(Gradient::fromValue),
            Codec.DOUBLE.optionalFieldOf("to_value", 1.0d).forGetter(Gradient::toValue)
        ).apply(i, Gradient::new));

        @Override
        public double compute(Context context) {
            double v = axis.pick(context.x(), context.y(), context.z());
            double t = to == from ? (v < from ? 0.0d : 1.0d) : Maths.clamp01((v - from) / (to - from));
            return fromValue + (toValue - fromValue) * t;
        }

        @Override
        public MapCodec<? extends NoiseFunction> codec() {
            return null;
        }

        @Override
        public NoiseFunction mapAll(Visitor visitor) {
            return visitor.apply(this);
        }

        @Override
        public NoiseFunction mapChildren(UnaryOperator<NoiseFunction> function) {
            return this;
        }
    }

    /// Distance-from-center ramp in the XY plane. innerValue inside innerRadius, outerValue beyond outerRadius.
    /// You can multiply or subtract it with a noise to get island masks.
    public record RadialGradient(double centerX, double centerY, double innerRadius, double outerRadius, double innerValue, double outerValue) implements NoiseFunction {
        public static final MapCodec<RadialGradient> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.optionalFieldOf("center_x", 0.0d).forGetter(RadialGradient::centerX),
            Codec.DOUBLE.optionalFieldOf("center_y", 0.0d).forGetter(RadialGradient::centerY),
            Codec.DOUBLE.fieldOf("inner_radius").forGetter(RadialGradient::innerRadius),
            Codec.DOUBLE.fieldOf("outer_radius").forGetter(RadialGradient::outerRadius),
            Codec.DOUBLE.optionalFieldOf("inner_value", -1.0d).forGetter(RadialGradient::innerValue),
            Codec.DOUBLE.optionalFieldOf("outer_value", 0.0d).forGetter(RadialGradient::outerValue)
        ).apply(i, RadialGradient::new));

        @Override
        public double compute(Context context) {
            double dx = context.x() - centerX;
            double dy = context.y() - centerY;
            double d = Math.sqrt(dx * dx + dy * dy);
            double t = outerRadius == innerRadius
                ? (d < innerRadius ? 0.0d : 1.0d)
                : Maths.clamp01((d - innerRadius) / (outerRadius - innerRadius));
            return innerValue + (outerValue - innerValue) * t;
        }

        @Override
        public MapCodec<? extends NoiseFunction> codec() {
            return CODEC;
        }

        @Override
        public NoiseFunction mapAll(Visitor visitor) {
            return visitor.apply(this);
        }

        @Override
        public NoiseFunction mapChildren(UnaryOperator<NoiseFunction> function) {
            return this;
        }
    }

    /// Quantizes a 0..1 input into `levels` plateaus (feed it through remap01 first).
    /// smoothness 0 = hard steps; larger values round the risers (smoothstep over the top `smoothness`
    /// fraction of each level). Output stays in 0..1 and is continuous when smoothness > 0.
    public record Terrace(NoiseFunction input, int levels, double smoothness) implements NoiseFunction {
        public static final MapCodec<Terrace> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            NoiseFunction.CODEC.fieldOf("input").forGetter(Terrace::input),
            Codec.intRange(2, 1024).fieldOf("levels").forGetter(Terrace::levels),
            Codec.doubleRange(0.0d, 1.0d).optionalFieldOf("smoothness", 0.25d).forGetter(Terrace::smoothness)
        ).apply(i, Terrace::new));

        @Override
        public double compute(Context context) {
            double t = input.compute(context) * levels;
            double base = Math.floor(t);
            double ramp = 0.0d;
            if (smoothness > 0.0d) {
                double frac = t - base;
                ramp = Maths.smoothstep(Maths.clamp01((frac - (1.0d - smoothness)) / smoothness));
            }
            return (base + ramp) / levels;
        }

        @Override
        public MapCodec<? extends NoiseFunction> codec() {
            return CODEC;
        }

        @Override
        public NoiseFunction mapAll(Visitor visitor) {
            return visitor.apply(new Terrace(input.mapAll(visitor), levels, smoothness));
        }

        @Override
        public NoiseFunction mapChildren(UnaryOperator<NoiseFunction> function) {
            NoiseFunction n0 = function.apply(input);
            return n0 == input ? this : new Terrace(n0, levels, smoothness);
        }
    }

    /// Spline remap through user-placed points (in -> out), cubic Hermite with Fritsch-Butland slopes,
    /// so the curve is smooth and never overshoots between points (a monotone input gives a monotone output).
    /// Values outside the first/last `in` are held at the end points' `out`.
    /// Slopes are computed per call from the two neighboring segments: no hidden arrays, so the record
    /// keeps structural equals/hashCode for node dedup.
    public record CurveRemap(NoiseFunction input, List<Point> points) implements NoiseFunction {
        public static final MapCodec<CurveRemap> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            NoiseFunction.CODEC.fieldOf("input").forGetter(CurveRemap::input),
            Point.CODEC.listOf().validate(list -> {
                String problem = findProblem(list);
                return problem == null ? DataResult.success(list) : DataResult.error(() -> problem);
            }).fieldOf("points").forGetter(CurveRemap::points)
        ).apply(i, CurveRemap::new));

        public record Point(double in, double out) {
            public static final Codec<Point> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("in").forGetter(Point::in),
                Codec.DOUBLE.fieldOf("out").forGetter(Point::out)
            ).apply(i, Point::new));
        }

        public CurveRemap {
            String problem = findProblem(points);
            if (problem != null) throw new IllegalArgumentException(problem);
            points = List.copyOf(points);
        }

        private static @Nullable String findProblem(List<Point> points) {
            if (points.size() < 2) return "curve needs at least 2 points";
            for (int k = 1; k < points.size(); k++) {
                if (!(points.get(k).in() > points.get(k - 1).in())) {
                    return "curve points need strictly increasing 'in' values";
                }
            }
            return null;
        }

        private double delta(int k) {
            Point a = points.get(k);
            Point b = points.get(k + 1);
            return (b.out() - a.out()) / (b.in() - a.in());
        }

        private double slope(int k) {
            int size = points.size();
            if (k == 0) return delta(0);
            if (k == size - 1) return delta(size - 2);
            double d0 = delta(k - 1);
            double d1 = delta(k);
            if (d0 * d1 <= 0.0d) return 0.0d; // local extremum or flat: keep it flat
            double h0 = points.get(k).in() - points.get(k - 1).in();
            double h1 = points.get(k + 1).in() - points.get(k).in();
            double w1 = 2.0d * h1 + h0;
            double w2 = h1 + 2.0d * h0;
            return (w1 + w2) / (w1 / d0 + w2 / d1);
        }

        @Override
        public double compute(Context context) {
            double v = input.compute(context);
            int size = points.size();
            Point first = points.getFirst();
            Point last = points.getLast();
            if (v <= first.in()) return first.out();
            if (v >= last.in()) return last.out();

            int lo = 0;
            int hi = size - 1;
            while (hi - lo > 1) {
                int mid = (lo + hi) >>> 1;
                if (points.get(mid).in() <= v) lo = mid;
                else hi = mid;
            }

            Point a = points.get(lo);
            Point b = points.get(lo + 1);
            double h = b.in() - a.in();
            double t = (v - a.in()) / h;
            double t2 = t * t;
            double t3 = t2 * t;
            return (2 * t3 - 3 * t2 + 1) * a.out()
                + (t3 - 2 * t2 + t) * h * slope(lo)
                + (-2 * t3 + 3 * t2) * b.out()
                + (t3 - t2) * h * slope(lo + 1);
        }

        @Override
        public MapCodec<? extends NoiseFunction> codec() {
            return CODEC;
        }

        @Override
        public NoiseFunction mapAll(Visitor visitor) {
            return visitor.apply(new CurveRemap(input.mapAll(visitor), points));
        }

        @Override
        public NoiseFunction mapChildren(UnaryOperator<NoiseFunction> function) {
            NoiseFunction n0 = function.apply(input);
            return n0 == input ? this : new CurveRemap(n0, points);
        }
    }

    /// Picks between two branches by comparing `selector` with `threshold`.
    /// edge = 0: hard switch, only the chosen branch is evaluated.
    /// edge > 0: smoothstep crossfade over [threshold - edge, threshold + edge]; both branches are only
    /// evaluated for pixels inside that band.
    public record Select(NoiseFunction selector, double threshold, double edge, NoiseFunction above, NoiseFunction below) implements NoiseFunction {
        public static final MapCodec<Select> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            NoiseFunction.CODEC.fieldOf("selector").forGetter(Select::selector),
            Codec.DOUBLE.fieldOf("threshold").forGetter(Select::threshold),
            Codec.doubleRange(0.0d, Double.MAX_VALUE).optionalFieldOf("edge", 0.0d).forGetter(Select::edge),
            NoiseFunction.CODEC.fieldOf("above").forGetter(Select::above),
            NoiseFunction.CODEC.fieldOf("below").forGetter(Select::below)
        ).apply(i, Select::new));

        @Override
        public double compute(Context context) {
            double s = selector.compute(context);
            double t = edge <= 0.0d
                ? (s >= threshold ? 1.0d : 0.0d)
                : Maths.smoothstep(Maths.clamp01((s - (threshold - edge)) / (2.0d * edge)));
            if (t < 0.0d) return below.compute(context);
            if (t >= 1.0d) return above.compute(context);
            double b = below.compute(context);
            double a = above.compute(context);
            return b + (a - b) * t;
        }

        @Override
        public MapCodec<? extends NoiseFunction> codec() {
            return CODEC;
        }

        @Override
        public NoiseFunction mapAll(Visitor visitor) {
            return visitor.apply(new Select(selector.mapAll(visitor), threshold, edge, above.mapAll(visitor), below.mapAll(visitor)));
        }

        @Override
        public NoiseFunction mapChildren(UnaryOperator<NoiseFunction> function) {
            NoiseFunction n0 = function.apply(selector);
            NoiseFunction n1 = function.apply(above);
            NoiseFunction n2 = function.apply(below);
            return n0 == selector && n1 == above && n2 == below ? this : new Select(n0, threshold, edge, n1, n2);
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
