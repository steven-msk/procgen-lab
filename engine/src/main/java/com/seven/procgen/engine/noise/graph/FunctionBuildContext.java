package com.seven.procgen.engine.noise.graph;

import com.google.common.collect.ImmutableMap;
import com.seven.procgen.engine.noise.function.NoiseFunction;

import java.util.Map;
import java.util.Optional;

public class FunctionBuildContext {
    private final Map<String, NoiseFunction> in;
    private final Map<String, Object> params;

    public FunctionBuildContext(Map<String, NoiseFunction> in, Map<String, Object> params) {
        this.in = ImmutableMap.copyOf(in);
        this.params = ImmutableMap.copyOf(params);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> optionalParam(String name) {
        Object v = params.get(name);
        if (v == null) return Optional.empty();
        return Optional.of((T)v);
    }

    @SuppressWarnings("unchecked")
    public <T> T param(String name) {
        return (T)optionalParam(name).orElseThrow();
    }

    public NoiseFunction pin(String name) {
        return Optional.ofNullable(in.get(name)).orElseThrow();
    }
}
