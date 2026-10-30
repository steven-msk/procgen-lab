package com.seven.procgen.engine;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.seven.procgen.engine.noise.function.NoiseFunction;
import com.seven.procgen.engine.noise.function.NoiseFunctions;
import com.seven.procgen.engine.noise.graph.NoiseGraph;
import com.seven.procgen.engine.noise.sampler.NoiseSamplerType;
import com.seven.procgen.engine.registry.Identifier;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.seven.procgen.engine.TestNoise.bind;
import static com.seven.procgen.engine.TestNoise.tree;
import static com.seven.procgen.engine.noise.function.NoiseFunctions.createSampler;
import static com.seven.procgen.engine.noise.function.NoiseFunctions.remap01;
import static org.junit.jupiter.api.Assertions.*;

public class NoiseFunctionSerializationTest {
    static JsonElement encode(NoiseFunction f) {
        return unwrap(NoiseGraph.TREE_CODEC.encodeStart(JsonOps.INSTANCE, f));
    }

    static NoiseFunction decode(JsonElement j) {
        return unwrap(NoiseGraph.TREE_CODEC.parse(JsonOps.INSTANCE, j));
    }

    static <T> T unwrap(DataResult<T> r) {
        return r.result().orElseThrow(() -> new IllegalStateException(r.error().map(DataResult.Error::message).orElse("unknown error")));
    }

    @Test
    void treeRoundTrips() {
        NoiseFunction original = tree();

        JsonElement json = encode(original);
        NoiseFunction decoded = decode(json);

        assertEquals(json, encode(decoded)); // canonical form is stable

        NoiseFunction a = bind(original, 12345L);
        NoiseFunction b = bind(decoded, 12345L);
        for (int x = -64; x < 64; x++) {
            for (int y = -64; y < 64; y++) {
                var ctx = new NoiseFunction.SinglePointContext(x, y, 0);
                assertEquals(a.compute(ctx), b.compute(ctx));
            }
        }
    }

    @Test
    void sharedNodesStayShared() {
        NoiseFunction plains = createSampler("plains", NoiseSamplerType.PERLIN, 0.02, 0.02, 1.0);
        NoiseFunction tree = plains.mul(0.5).add(remap01(plains));

        assertEquals(Set.of(Identifier.of("plains")), NoiseGraph.of(tree).definitions().keySet());

        NoiseFunction decoded = decode(encode(tree)); // using TREE_CODEC
        assertEquals(tree, decoded); // aka same structure
        var add = (NoiseFunctions.BinaryFunction)decoded;
        assertSame(((NoiseFunctions.BinaryFunction)add.a()).a(), ((NoiseFunctions.Affine)add.b()).input());   // one instance, not two copies
    }

    @Test
    void cycleIsRejected() {
        // a -> ref b, b -> ref a; root: ref a
        String cyclicJson = """
            {
              "format": 1,
              "definitions": {
                "a": { "type": "unary_function", "operation": "ABS", "input": { "type": "reference", "id": "b" } },
                "b": { "type": "unary_function", "operation": "ABS", "input": { "type": "reference", "id": "a" } }
              },
              "root": { "type": "reference", "id": "a" }
            }
            """;
        assertTrue(NoiseGraph.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(cyclicJson)).result().isPresent());
        assertTrue(parseError(cyclicJson).contains("a -> b -> a"));
    }

    @Test
    void undefinedRefIsRejected() {
        // root: ref "nope", and nothing defines it
        String json = """
            {
              "format": 1,
              "root": { "type": "reference", "id": "nope" }
            }
            """;
        String error = parseError(json);
        assertTrue(error.contains("Undefined reference"), error);
        assertTrue(error.contains("nope"), error);
    }

    /// Parses through TREE_CODEC (graph decode + ref resolution) and returns the error message. Fails if parsing succeeds.
    private static String parseError(String json) {
        DataResult<NoiseFunction> result = NoiseGraph.TREE_CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json));
        return result.error()
            .map(DataResult.Error::message)
            .orElseThrow(() -> new AssertionError("Expected a parse error but got: " + result.result().orElse(null)));
    }
}
