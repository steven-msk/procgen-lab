package com.seven.procgen.engine.noise.graph;

import com.mojang.serialization.MapCodec;
import com.seven.procgen.engine.noise.function.NoiseFunction;
import com.seven.procgen.engine.noise.function.NoiseFunctionTypes;
import com.seven.procgen.engine.noise.function.NoiseFunctions;
import com.seven.procgen.engine.registry.*;
import com.seven.procgen.engine.util.math.Axis;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class NodeSpecs {
    private NodeSpecs() {
    }

    public static NodeSpec init(Registry<NodeSpec> registry, RegistryEntryLookup.RegistryLookup registryLookup) {
        return new Bootstrap(registry, registryLookup).init();
    }

    public static RegistryKey<NodeSpec> keyOf(RegistryKey<MapCodec<? extends NoiseFunction>> functionKey) {
        return RegistryKey.of(Registries.NODE_SPEC, functionKey.value());
    }

    public static RegistryKey<NodeSpec> keyOf(NoiseFunction function) {
        return switch (function) {
            case NoiseFunctions.UnaryFunction u -> getUnaryKey(u.operation());
            case NoiseFunctions.BinaryFunction b -> getBinaryKey(b.operation());
            default -> keyOf(Registry.NOISE_FUNCTION.getKey(function.codec()).orElseThrow());
        };
    }

    public static RegistryKey<NodeSpec> getUnaryKey(NoiseFunctions.UnaryOperation operation) {
        return getKey(operation.name().toLowerCase(Locale.ROOT));
    }

    public static RegistryKey<NodeSpec> getBinaryKey(NoiseFunctions.BinaryOperation operation) {
        return getKey(operation.name().toLowerCase(Locale.ROOT));
    }

    private static RegistryKey<NodeSpec> getKey(String id) {
        return RegistryKey.of(Registries.NODE_SPEC, Identifier.of(id));
    }



    private record Bootstrap(Registry<NodeSpec> registry, RegistryEntryLookup.RegistryLookup registryLookup) {
        public NodeSpec init() {
            registerUnaryNodes();
            registerBinaryNodes();
            registerFunction(NoiseFunctionTypes.CLAMP, NodeSpec.of("Clamp", NodeSpec.Category.MATH,
                List.of("input"),
                List.of(
                    ParamSpec.unboundedDouble("min", -1.0d),
                    ParamSpec.unboundedDouble("max", 1.0d)
                ),
                c -> NoiseFunctions.clamp(c.pin("input"), c.param("min"), c.param("max")),
                NoiseFunctions.Clamp.class,
                f -> FunctionDecompose.of().pin("input", f.input())
                    .param("min", f.min()).param("max", f.max())
            ));
            registerFunction(NoiseFunctionTypes.LERP, NodeSpec.of("Lerp", NodeSpec.Category.MATH,
                List.of("from", "to", "t"), List.of(),
                c -> NoiseFunctions.lerp(c.pin("from"), c.pin("to"), c.pin("t")),
                NoiseFunctions.Lerp.class,
                f -> FunctionDecompose.of().pin("from", f.from()).pin("to", f.to()).pin("t", f.t())
            ));
            registerFunction(NoiseFunctionTypes.AFFINE, NodeSpec.of("Affine", NodeSpec.Category.MATH,
                List.of("input"),
                List.of(
                    ParamSpec.unboundedDouble("scale", 0.5d),
                    ParamSpec.unboundedDouble("bias", 0.5d)
                ),
                c -> NoiseFunctions.affine(
                    c.pin("input"), c.param("scale"), c.param("bias")
                ),
                NoiseFunctions.Affine.class,
                f -> FunctionDecompose.of().pin("input", f.input())
                    .param("scale", f.scale()).param("bias", f.bias())
            ));

            registerFunction(NoiseFunctionTypes.SCALE, NodeSpec.of("Scale", NodeSpec.Category.COORDINATE,
                List.of("input"),
                List.of(
                    ParamSpec.unboundedDouble("x", 1.0d),
                    ParamSpec.unboundedDouble("y", 1.0d),
                    ParamSpec.unboundedDouble("z", 1.0d)
                ),
                c -> NoiseFunctions.scale(
                    c.pin("input"), c.param("x"), c.param("y"), c.param("z")
                ),
                NoiseFunctions.Scale.class,
                f -> FunctionDecompose.of().pin("input", f.input())
                    .param("x", f.x()).param("y", f.y()).param("z", f.z())
            ));
            registerFunction(NoiseFunctionTypes.OFFSET, NodeSpec.of("Offset", NodeSpec.Category.COORDINATE,
                List.of("input"),
                List.of(
                    ParamSpec.unboundedDouble("dx", 0.0d),
                    ParamSpec.unboundedDouble("dy", 0.0d),
                    ParamSpec.unboundedDouble("dz", 0.0d)
                ),
                c -> NoiseFunctions.offset(
                    c.pin("input"), c.param("dx"), c.param("dy"), c.param("dz")
                ),
                NoiseFunctions.Offset.class,
                f -> FunctionDecompose.of().pin("input", f.input())
                    .param("dx", f.dx()).param("dy", f.dy()).param("dz", f.dz())
            ));
            registerFunction(NoiseFunctionTypes.ROTATE, NodeSpec.of("Rotate", NodeSpec.Category.COORDINATE,
                List.of("input"),
                List.of(
                    ParamSpec.unboundedDouble("degrees", 45.0d),
                    ParamSpec.unboundedDouble("pivot_x", 0.0d),
                    ParamSpec.unboundedDouble("pivot_y", 0.0d)
                ),
                c -> NoiseFunctions.rotate(
                    c.pin("input"), c.param("degrees"), c.param("pivot_x"), c.param("pivot_y")
                ),
                NoiseFunctions.Rotate.class,
                f -> FunctionDecompose.of().pin("input", f.input()).param("degrees", f.degrees())
                    .param("pivot_x", f.pivotX()).param("pivot_y", f.pivotY())
            ));
            registerFunction(NoiseFunctionTypes.DOMAIN_WARP, NodeSpec.of("Domain Warp", NodeSpec.Category.COORDINATE,
                List.of("input", "offset_x", "offset_y"),
                List.of(ParamSpec.unboundedDouble("amplitude", 1.0d)),
                c -> NoiseFunctions.warp(
                    c.pin("input"), c.pin("offset_x"), c.pin("offset_y"), c.param("amplitude")
                ),
                NoiseFunctions.DomainWarp.class,
                f -> FunctionDecompose.of().pin("input", f.input())
                    .pin("offset_x", f.offsetX()).pin("offset_y", f.offsetY())
                    .param("amplitude", f.amplitude())
            ));

            registerFunction(NoiseFunctionTypes.GRADIENT, NodeSpec.of("Gradient", NodeSpec.Category.REMAP,
                List.of(),
                List.of(
                    ParamSpec.enumValue("axis", Axis.Y),
                    ParamSpec.unboundedDouble("from", -100.0d),
                    ParamSpec.unboundedDouble("to", 100.0d),
                    ParamSpec.unboundedDouble("from_value", -1.0d),
                    ParamSpec.unboundedDouble("to_value", 1.0d)
                ),
                c -> NoiseFunctions.gradient(
                    c.param("axis"), c.param("from"), c.param("to"),
                    c.param("from_value"), c.param("to_value")
                ),
                NoiseFunctions.Gradient.class,
                f -> FunctionDecompose.of().param("axis", f.axis())
                    .param("from", f.from()).param("to", f.to())
                    .param("from_value", f.fromValue()).param("to_value", f.toValue())
            ));
            registerFunction(NoiseFunctionTypes.RADIAL_GRADIENT, NodeSpec.of("Radial Gradient", NodeSpec.Category.REMAP,
                List.of(),
                List.of(
                    ParamSpec.unboundedDouble("center_x", 0.0d),
                    ParamSpec.unboundedDouble("center_y", 0.0d),
                    ParamSpec.unboundedDouble("inner_radius", 50.0d),
                    ParamSpec.unboundedDouble("outer_radius", 500.0d),
                    ParamSpec.unboundedDouble("inner_value", -1.0d),
                    ParamSpec.unboundedDouble("outer_value", 1.0d)
                ),
                c -> NoiseFunctions.radialGradient(
                    c.param("center_x"), c.param("center_y"),
                    c.param("inner_radius"), c.param("outer_radius"),
                    c.param("inner_value"), c.param("outer_value")
                ),
                NoiseFunctions.RadialGradient.class,
                f -> FunctionDecompose.of().param("center_x", f.centerX()).param("center_y", f.centerY())
                    .param("inner_radius", f.innerRadius()).param("outer_radius", f.outerRadius())
                    .param("inner_value", f.innerValue()).param("outer_value", f.outerValue())
            ));
            registerFunction(NoiseFunctionTypes.TERRACE, NodeSpec.of("Terrace", NodeSpec.Category.REMAP,
                List.of("input"),
                List.of(
                    ParamSpec.rangedInt("levels", 12, 2, 1024),
                    ParamSpec.rangedDouble("smoothness", 0.25d, 0.0d, 1.0d)
                ),
                c -> NoiseFunctions.terrace(
                    c.pin("input"), c.param("levels"), c.param("smoothness")
                ),
                NoiseFunctions.Terrace.class,
                f -> FunctionDecompose.of().pin("input", f.input())
                    .param("levels", f.levels()).param("smoothness", f.smoothness())
            ));
            registerFunction(NoiseFunctionTypes.CURVE_REMAP, NodeSpec.of("Curve Remap", NodeSpec.Category.REMAP,
                List.of("input"),
                List.of(CurvePointsParam.INSTANCE),
                c -> NoiseFunctions.curveRemapWithPoints(c.pin("input"), c.param(CurvePointsParam.INSTANCE.name())),
                NoiseFunctions.CurveRemap.class,
                f -> FunctionDecompose.of().pin("input", f.input()).param(CurvePointsParam.INSTANCE.name(), f.points())
            ));
            registerFunction(NoiseFunctionTypes.SELECT, NodeSpec.of("Select", NodeSpec.Category.REMAP,
                List.of("selector", "above", "below"),
                List.of(
                    ParamSpec.unboundedDouble("threshold", 0.0d),
                    ParamSpec.rangedDouble("edge", 0.0d, 0.0d, Double.MAX_VALUE)
                ),
                c -> NoiseFunctions.select(
                    c.pin("selector"), c.param("threshold"),
                    c.param("edge"), c.pin("above"), c.pin("below")
                ),
                NoiseFunctions.Select.class,
                f -> FunctionDecompose.of().pin("selector", f.selector())
                    .pin("above", f.above()).pin("below", f.below())
                    .param("threshold", f.threshold()).param("edge", f.edge())
            ));

            return registerFunction(NoiseFunctionTypes.CONSTANT, NodeSpec.of("Constant", NodeSpec.Category.MATH,
                List.of(),
                List.of(ParamSpec.unboundedDouble("value", 0.0d)),
                c -> NoiseFunctions.constant(c.param("value")),
                NoiseFunctions.Constant.class,
                f -> FunctionDecompose.of().param("value", f.value())
            ));
        }

        private void registerUnaryNodes() {
            registerUnary(NoiseFunctions.UnaryOperation.ABS, "Abs", NoiseFunctions::abs);
            registerUnary(NoiseFunctions.UnaryOperation.NEGATE, "Negate", NoiseFunctions::negate);
            registerUnary(NoiseFunctions.UnaryOperation.SQUARE, "Square", NoiseFunctions::square);
            registerUnary(NoiseFunctions.UnaryOperation.CUBE, "Cube", NoiseFunctions::cube);
            registerUnary(NoiseFunctions.UnaryOperation.ONE_MINUS, "One Minus", NoiseFunctions::oneMinus);
        }

        private void registerBinaryNodes() {
            registerBinary(NoiseFunctions.BinaryOperation.ADD, "Add", NoiseFunctions::add);
            registerBinary(NoiseFunctions.BinaryOperation.SUBTRACT, "Subtract", NoiseFunctions::subtract);
            registerBinary(NoiseFunctions.BinaryOperation.MULTIPLY, "Multiply", NoiseFunctions::mul);
            registerBinary(NoiseFunctions.BinaryOperation.MIN, "Min", NoiseFunctions::min);
            registerBinary(NoiseFunctions.BinaryOperation.MAX, "Max", NoiseFunctions::max);
        }

        private void registerUnary(NoiseFunctions.UnaryOperation operation, String name, Function<NoiseFunction, NoiseFunction> factory) {
            register(getUnaryKey(operation), NodeSpec.of(name, NodeSpec.Category.MATH,
                List.of("input"),
                List.of(),
                c -> factory.apply(c.pin("input")),
                NoiseFunctions.UnaryFunction.class,
                f -> f.operation() == operation ? FunctionDecompose.of().pin("input", f.input()) : null
            ));
        }

        private void registerBinary(NoiseFunctions.BinaryOperation operation, String name, BiFunction<NoiseFunction, NoiseFunction, NoiseFunction> factory) {
            register(getBinaryKey(operation), NodeSpec.of(name, NodeSpec.Category.MATH,
                List.of("a", "b"),
                List.of(),
                c -> factory.apply(c.pin("a"), c.pin("b")),
                NoiseFunctions.BinaryFunction.class,
                f -> f.operation() == operation ? FunctionDecompose.of().pin("a", f.a()).pin("b", f.b()) : null
            ));
        }

        private NodeSpec registerFunction(RegistryKey<MapCodec<? extends NoiseFunction>> functionKey, NodeSpec spec) {
            RegistryKey<NodeSpec> specKey = keyOf(functionKey);
            return register(specKey, spec);
        }

        private NodeSpec register(RegistryKey<NodeSpec> key, NodeSpec spec) {
            return Registry.register(registry, key, spec);
        }
    }
}
