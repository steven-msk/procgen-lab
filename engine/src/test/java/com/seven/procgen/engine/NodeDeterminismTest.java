package com.seven.procgen.engine;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.seven.procgen.engine.noise.function.NoiseFunction;
import com.seven.procgen.engine.util.math.Axis;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static com.seven.procgen.engine.noise.function.NoiseFunctions.*;

/// One case per node (and per meaningful parameter regime). Each node is tested on its own,
/// fed by analytic children (gradients) so no sampler binding or registry state is involved in the compute path.
class NodeDeterminismTest {
    /// Not a Context from the engine, to prove nodes only depend on x/y/z and not on the context class
    private record PlainContext(double x, double y, double z) implements NoiseFunction.Context {
    }

    // ---- analytic children: pure functions of x, y, z that don't saturate across the sample magnitudes

    @BeforeAll
    static void setup() {
        TestNoise.ensureSetup();
    }

    private static NoiseFunction gx() {
        return gradient(Axis.X, -1000.0d, 1000.0d, -1.0d, 1.0d);
    }

    private static NoiseFunction gy() {
        return gradient(Axis.Y, -1000.0d, 1000.0d, -1.0d, 1.0d);
    }

    private static NoiseFunction gz() {
        return gradient(Axis.Z, -3.0d, 3.0d, -1.0d, 1.0d);
    }

    /// Depends on all three axes, range [-2, 2]
    private static NoiseFunction signal() {
        return add(mul(gx(), gy()), gz());
    }

    /// signal squeezed into [0, 1]
    private static NoiseFunction unit() {
        return remap01(clamp(signal(), -1.0d, 1.0d));
    }

    private static Arguments c(String name, Supplier<NoiseFunction> factory) {
        return Arguments.of(name, factory);
    }

    static Stream<Arguments> nodes() {
        List<Arguments> cases = new ArrayList<>();

        cases.add(c("constant", () -> constant(0.37d)));
        for (UnaryOperation op : UnaryOperation.values())
            cases.add(c("unary " + op, () -> new UnaryFunction(op, signal())));
        for (BinaryOperation op : BinaryOperation.values())
            cases.add(c("binary " + op, () -> new BinaryFunction(op, signal(), unit())));
        cases.add(c("affine", () -> affine(signal(), 0.5d, 0.5d)));
        cases.add(c("clamp", () -> clamp(signal(), -0.5d, 0.5d)));
        cases.add(c("lerp", () -> lerp(signal(), unit(), unit())));

        // ---- coordinate nodes
        cases.add(c("scale uniform", () -> scale(signal(), 2.5d)));
        cases.add(c("scale per axis", () -> scale(signal(), 0.5d, 3.0d, 0.25d)));
        cases.add(c("offset", () -> offset(signal(), 12.5d, -7.25d, 0.5d)));
        cases.add(c("rotate about origin", () -> rotate(signal(), 37.0d)));
        cases.add(c("rotate about pivot", () -> rotate(signal(), -121.5d, 40.0d, -15.0d)));
        cases.add(c("warp", () -> warp(signal(), gx(), gy(), 25.0d)));
        cases.add(c("warp, offsets are transformed copies", () -> warp(signal(), offset(gx(), 5.0d, 9.0d, 0.0d), scale(gy(), 0.5d), 25.0d)));

        // ---- generators
        for (Axis axis : Axis.values())
            cases.add(c("gradient " + axis, () -> gradient(axis, -100.0d, 100.0d, -1.0d, 1.0d)));
        cases.add(c("gradient descending", () -> gradient(Axis.Y, 100.0d, -100.0d, -1.0d, 1.0d)));
        cases.add(c("gradient degenerate from == to", () -> gradient(Axis.X, 5.0d, 5.0d, -1.0d, 1.0d)));
        cases.add(c("radial gradient", () -> radialGradient(10.0d, -20.0d, 50.0d, 400.0d, 0.25d, -1.0d)));
        cases.add(c("radial gradient degenerate radii", () -> radialGradient(0.0d, 0.0d, 30.0d, 30.0d, 1.0d, -1.0d)));

        // ---- remap nodes
        cases.add(c("terrace hard", () -> terrace(unit(), 8, 0.0d)));
        cases.add(c("terrace smooth", () -> terrace(unit(), 8, 0.35d)));
        cases.add(c("terrace smoothness 1", () -> terrace(unit(), 2, 1.0d)));
        cases.add(c("curve 2 points", () -> curveRemap(clamp(signal(), -1.0d, 1.0d), b -> b.point(-1.0d, -1.0d).point(1.0d, 1.0d))));
        cases.add(c("curve monotone", () -> curveRemap(clamp(signal(), -1.0d, 1.0d), b -> b
            .point(-1.0d, -1.0d).point(-0.3d, -0.6d).point(0.0d, 0.0d).point(0.3d, 0.2d).point(0.6d, 0.5d).point(1.0d, 1.0d))));
        cases.add(c("curve with local extremum", () -> curveRemap(clamp(signal(), -1.0d, 1.0d), b -> b
            .point(-1.0d, 0.0d).point(-0.4d, 1.0d).point(0.2d, -0.5d).point(1.0d, 0.5d))));
        cases.add(c("select hard", () -> select(signal(), 0.1d, unit(), gx())));
        cases.add(c("select soft", () -> select(signal(), 0.1d, 0.4d, unit(), gx())));
        cases.add(c("select wide band", () -> select(signal(), 0.0d, 5.0d, unit(), gx())));

        // ---- composite built only from the nodes above
        cases.add(c("composite", () -> {
            NoiseFunction warped = warp(rotate(scale(signal(), 0.7d), 20.0d), gx(), gy(), 30.0d);
            NoiseFunction land = select(gz(), 0.05d, 0.2d, warped, unit());
            NoiseFunction terraced = terrace(remap01(clamp(land, -1.0d, 1.0d)), 12, 0.3d);
            NoiseFunction shaped = curveRemap(clamp(affine(terraced, 2.0d, -1.0d), -1.0d, 1.0d),
                b -> b.point(-1.0d, -1.0d).point(0.0d, 0.1d).point(1.0d, 1.0d));
            return add(shaped, radialGradient(0.0d, 0.0d, 100.0d, 800.0d, 0.25d, -1.0d));
        }));

        return cases.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nodes")
    void producesIdenticalOutputEveryTime(String name, Supplier<NoiseFunction> factory) {
        NoiseFunction a = factory.get();
        NoiseFunction b = factory.get();

        // independently built equal trees must be equal and hash equal (node dedup relies on it)
        Assertions.assertEquals(a, b);
        Assertions.assertEquals(a.hashCode(), b.hashCode());

        NoiseDeterminism.assertDeterministic(
            (x, y, z) -> a.compute(new NoiseFunction.SinglePointContext(x, y, z)),
            (x, y, z) -> b.compute(new PlainContext(x, y, z))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nodes")
    void survivesCodecRoundTrip(String name, Supplier<NoiseFunction> factory) {
        NoiseFunction original = factory.get();
        JsonElement json = NoiseFunction.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        NoiseFunction decoded = NoiseFunction.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();

        Assertions.assertEquals(original, decoded);
        NoiseDeterminism.assertSameBits("decoded tree",
            NoiseDeterminism.run((x, y, z) -> original.compute(new NoiseFunction.SinglePointContext(x, y, z)), NoiseDeterminism.forward()),
            NoiseDeterminism.run((x, y, z) -> decoded.compute(new NoiseFunction.SinglePointContext(x, y, z)), NoiseDeterminism.forward()));
    }

    /// NoiseProbes rebuilds trees through mapChildren and relies on "same instance if nothing changed"
    @ParameterizedTest(name = "{0}")
    @MethodSource("nodes")
    void mapChildrenWithIdentityKeepsTheInstance(String name, Supplier<NoiseFunction> factory) {
        NoiseFunction node = factory.get();
        Assertions.assertSame(node, node.mapChildren(UnaryOperator.identity()));
    }
}