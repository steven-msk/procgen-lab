package com.seven.procgen.engine.noise.graph;

public interface ParamSpec<T> {
    String name();

    T defaultValue();

    static ParamSpec<Double> rangedDouble(String name, double defaultValue, double minInclusive, double maxInclusive) {
        return new DoubleSpec(name, defaultValue, minInclusive, maxInclusive);
    }

    static ParamSpec<Double> unboundedDouble(String name, double defaultValue) {
        return rangedDouble(name, defaultValue, -Double.MAX_VALUE, Double.MAX_VALUE);
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
}
