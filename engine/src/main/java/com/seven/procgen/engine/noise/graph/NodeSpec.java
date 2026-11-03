package com.seven.procgen.engine.noise.graph;

import com.google.common.collect.ImmutableList;
import com.seven.procgen.engine.noise.function.NoiseFunction;
import com.seven.procgen.engine.registry.Registries;
import com.seven.procgen.engine.registry.RegistryEntry;
import com.seven.procgen.engine.registry.RegistryEntryLookup;
import com.seven.procgen.engine.registry.RegistryKey;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface NodeSpec {
    String title();

    /// Used for the search popup
    Category category();

    /// Function inputs, in display order
    List<String> inputs();

    List<ParamSpec<?>> params();

    NoiseFunction build(FunctionBuildContext context);

    /// Inverse of build(); the pins and params that would rebuild 'function',
    /// or null if this spec did not produce it
    @Nullable FunctionDecompose decompose(NoiseFunction function);

    default double inputDefault(String pin) {
        return 0.0d;
    }

    /// The spec that produced 'function', with its decomposed pins and params.
    /// Null if none claims it.
    static NodeSpec.@Nullable Entry findMatch(NoiseFunction function, RegistryEntryLookup.RegistryLookup registryLookup) {
        RegistryKey<NodeSpec> key = NodeSpecs.keyOf(function);
        RegistryEntry<NodeSpec> entry = registryLookup.getOptionalEntry(Registries.NODE_SPEC, key).orElseThrow();
        FunctionDecompose decomposed = entry.value().decompose(function);
        return decomposed == null ? null : new Entry(entry, decomposed);
    }

    /// Only the output node should use this as this does not produce FunctionDecomposeContext
    static NodeSpec of(String title, Category category, List<String> inputs, List<ParamSpec<?>> params, Function<FunctionBuildContext, NoiseFunction> builder) {
        return of(title, category, inputs, params, builder, NoiseFunction.class, f -> null);
    }

    static <F extends NoiseFunction> NodeSpec of(
        String title, Category category,
        List<String> inputs, List<ParamSpec<?>> params,
        Function<FunctionBuildContext, NoiseFunction> builder, Class<F> functionType,
        Function<F, @Nullable FunctionDecompose> decomposer
    ) {
        List<String> inputsImmutable = ImmutableList.copyOf(inputs);
        List<ParamSpec<?>> paramsImmutable = ImmutableList.copyOf(params);
        Set<String> pinNames = Set.copyOf(inputsImmutable);
        Set<String> paramNames = paramsImmutable.stream().map(ParamSpec::name).collect(Collectors.toSet());
        return new NodeSpec() {
            @Override
            public @Nullable FunctionDecompose decompose(NoiseFunction function) {
                if (!functionType.isInstance(function)) return null;
                FunctionDecompose decomposed = decomposer.apply(functionType.cast(function));
                if (decomposed == null) return null;
                if (!decomposed.pins().keySet().equals(pinNames)) {
                    throw new IllegalStateException(title + ": decomposed pins " + decomposed.pins().keySet() + " != spec pins " + pinNames);
                }
                if (!decomposed.params().keySet().equals(paramNames)) {
                    throw new IllegalStateException(title + ": decomposed params " + decomposed.params().keySet() + " != spec params " + paramNames);
                }
                return decomposed;
            }

            @Override
            public String title() {
                return title;
            }

            @Override
            public Category category() {
                return category;
            }

            @Override
            public List<String> inputs() {
                return inputsImmutable;
            }

            @Override
            public List<ParamSpec<?>> params() {
                return paramsImmutable;
            }

            @Override
            public NoiseFunction build(FunctionBuildContext context) {
                return builder.apply(context);
            }
        };
    }

    enum Category {
        NOISE("Noise"),
        MATH("Math"),
        COORDINATE("Coordinate"),
        REMAP("Remap"),
        OUTPUT("Output"); // editor's own Out node, does not show in any popup

        private final String label;

        Category(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    record Entry(RegistryEntry<NodeSpec> registryEntry, FunctionDecompose decomposed) {
    }
}
