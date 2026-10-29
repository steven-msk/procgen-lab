package com.seven.procgen.engine;

import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.seven.procgen.engine.noise.function.NoiseFunction;
import com.seven.procgen.engine.registry.Registry;
import org.junit.jupiter.api.Test;

import static com.seven.procgen.engine.NoiseDeterminismTest.tree;
import static com.seven.procgen.engine.TestNoise.bind;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class NoiseFunctionSerializationTest {
    static JsonElement encode(NoiseFunction f) {
        return unwrap(NoiseFunction.CODEC.encodeStart(JsonOps.INSTANCE, f));
    }

    static NoiseFunction decode(JsonElement j) {
        return unwrap(NoiseFunction.CODEC.parse(JsonOps.INSTANCE, j));
    }

    static <T> T unwrap(DataResult<T> r) {
        return r.result().orElseThrow(() ->
            new IllegalStateException(r.error().map(DataResult.Error::message).orElse("unknown error")));
    }

    @Test
    void treeRoundTrips() {
        Registry.bootstrap();
        NoiseFunction original = tree();

        JsonElement json = encode(original);
        NoiseFunction decoded = decode(json);

        assertEquals(json, encode(decoded));            // canonical form is stable

        NoiseFunction a = bind(original, 12345L);
        NoiseFunction b = bind(decoded, 12345L);
        for (int x = -64; x < 64; x++)
            for (int y = -64; y < 64; y++) {
                var ctx = new NoiseFunction.SinglePointContext(x, y, 0);
                assertEquals(a.compute(ctx), b.compute(ctx));
            }
    }
}
