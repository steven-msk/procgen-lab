package com.seven.procgen.engine.noise.graph;

public interface ParamSpec<T> {
    String name();

    T defaultValue();

    /// Default for a freshly added node. Most params ignore the id
    default T defaultFor(int nodeId) {
        return defaultValue();
    }

    static ParamSpec<Double> rangedDouble(String name, double defaultValue, double minInclusive, double maxInclusive) {
        return new DoubleSpec(name, defaultValue, minInclusive, maxInclusive);
    }

    static ParamSpec<Double> unboundedDouble(String name, double defaultValue) {
        return rangedDouble(name, defaultValue, -Double.MAX_VALUE, Double.MAX_VALUE);
    }

    static ParamSpec<double[]> doubleArray(String name, double[] defaultValue, int minLength, int maxLength) {
        return new DoubleArraySpec(name, defaultValue, minLength, maxLength);
    }

    static ParamSpec<Integer> rangedInt(String name, int defaultValue, int minInclusive, int maxInclusive) {
        return new IntSpec(name, defaultValue, minInclusive, maxInclusive);
    }

    static ParamSpec<Integer> unboundedInt(String name, int defaultValue) {
        return new IntSpec(name, defaultValue, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    static <E extends Enum<E>> ParamSpec<E> enumValue(String name, E defaultValue) {
        return new EnumSpec<>(name, defaultValue);
    }

    static ParamSpec<String> string(String name, String defaultValue) {
        return new StringSpec(name, defaultValue, false);
    }

    static ParamSpec<String> uniqueString(String name, String prefix) {
        return new StringSpec(name, prefix, true);
    }

    static <T> ParamSpec<T> of(String name, T defaultValue) {
        return new Simple<>(name, defaultValue);
    }

    record DoubleSpec(String name, Double defaultValue, double minInclusive, double maxInclusive) implements ParamSpec<Double> {
        public DoubleSpec {
            if (defaultValue < minInclusive)
                throw new IllegalArgumentException("default value < minInclusive: " + defaultValue + " < " + minInclusive);
            if (defaultValue > maxInclusive)
                throw new IllegalArgumentException("default value > maxInclusive: " + defaultValue + " > " + maxInclusive);
        }
    }

    record IntSpec(String name, Integer defaultValue, int minInclusive, int maxInclusive) implements ParamSpec<Integer> {
        public IntSpec {
            if (defaultValue < minInclusive)
                throw new IllegalArgumentException("default value < minInclusive: " + defaultValue + " < " + minInclusive);
            if (defaultValue > maxInclusive)
                throw new IllegalArgumentException("default value > maxInclusive: " + defaultValue + " > " + maxInclusive);
        }
    }

    record EnumSpec<E extends Enum<E>>(String name, E defaultValue) implements ParamSpec<E> {
    }

    record Simple<T>(String name, T defaultValue) implements ParamSpec<T> {
    }

    /// Default is `prefix_<nodeId>`, so two nodes in one graph don't silently share a seed
    record StringSpec(String name, String defaultValue, boolean unique) implements ParamSpec<String> {
        @Override
        public String defaultFor(int nodeId) {
            return unique ? defaultValue + "_" + nodeId : defaultValue;
        }
    }

    record DoubleArraySpec(String name, double[] defaultValue, int minLength, int maxLength) implements ParamSpec<double[]> {
        public DoubleArraySpec {
            if (minLength < 1 || maxLength < minLength)
                throw new IllegalArgumentException("bad length range: " + minLength + ".." + maxLength);
            if (defaultValue.length < minLength || defaultValue.length > maxLength)
                throw new IllegalArgumentException("default length " + defaultValue.length + " outside " + minLength + ".." + maxLength);
            defaultValue = defaultValue.clone();
        }

        @Override
        public double[] defaultValue() {
            return defaultValue.clone();
        }
    }
}
