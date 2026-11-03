package com.seven.procgen.engine.noise.graph;

import com.seven.procgen.engine.noise.function.NoiseFunction;

import java.util.LinkedHashMap;
import java.util.Map;

public final class FunctionDecompose {
    private final Map<String, NoiseFunction> pins = new LinkedHashMap<>();
    private final Map<String, Object> params = new LinkedHashMap<>();

    public static FunctionDecompose of() {
        return new FunctionDecompose();
    }

    public FunctionDecompose pin(String name, NoiseFunction function) {
        pins.put(name, function);
        return this;
    }

    public <T> FunctionDecompose param(String name, T value) {
        params.put(name, value);
        return this;
    }

    public Map<String, NoiseFunction> pins() {
        return pins;
    }

    public Map<String, Object> params() {
        return params;
    }
}
