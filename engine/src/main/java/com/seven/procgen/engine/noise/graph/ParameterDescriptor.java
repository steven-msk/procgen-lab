package com.seven.procgen.engine.noise.graph;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.function.Function;

public interface ParameterDescriptor<T> {
    List<ParamSpec<?>> specs();

    T create(FunctionBuildContext context);

    /// Must produce exactly the names in specs()
    FunctionDecompose decompose(T parameters);

    static <T> ParameterDescriptor<T> of(List<ParamSpec<?>> specs, Function<FunctionBuildContext, T> factory, Function<T, FunctionDecompose> decomposer) {
        List<ParamSpec<?>> specsImmutable = ImmutableList.copyOf(specs);
        return new ParameterDescriptor<T>() {
            @Override
            public List<ParamSpec<?>> specs() {
                return specsImmutable;
            }

            @Override
            public T create(FunctionBuildContext context) {
                return factory.apply(context);
            }

            @Override
            public FunctionDecompose decompose(T parameters) {
                return decomposer.apply(parameters);
            }
        };
    }

    static <T> ParameterDescriptor<T> none(T parameters) {
        return of(List.of(), c -> parameters, p -> FunctionDecompose.of());
    }
}
