package com.seven.procgen.engine.noise.graph;

import com.google.common.collect.ImmutableList;
import com.seven.procgen.engine.noise.function.NoiseFunction;

import java.util.List;
import java.util.function.Function;

public interface NodeSpec {
    String title();

    /// Used for the search popup
    Category category();

    /// Function inputs, in display order
    List<String> inputs();

    List<ParamSpec<?>> params();

    NoiseFunction build(FunctionBuildContext context);

    default double inputDefault(String pin) {
        return 0.0d;
    }

    static NodeSpec of(String title, Category category, List<String> inputs, List<ParamSpec<?>> params, Function<FunctionBuildContext, NoiseFunction> builder) {
        List<String> inputsImmutable = ImmutableList.copyOf(inputs);
        List<ParamSpec<?>> paramsImmutable = ImmutableList.copyOf(params);
        return new NodeSpec() {
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
}
