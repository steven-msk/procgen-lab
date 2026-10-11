package com.seven.procgen.engine.random;

import java.util.Random;

public final class JavaRandomSource implements RandomSource {
    private final Random random = new Random();

    public JavaRandomSource(long seed) {
        this.setSeed(seed);
    }

    @Override
    public void setSeed(long seed) {
        // java.util.Random only lightly scrambles its seed, so consecutive seeds
        // (0, 1, 2...) give correlated first values. Mixing first avoids that.
        random.setSeed(Seeds.mix(seed));
    }

    @Override
    public boolean nextBool() {
        return random.nextBoolean();
    }

    @Override
    public double nextDouble() {
        return random.nextDouble();
    }

    @Override
    public float nextFloat() {
        return random.nextFloat();
    }

    @Override
    public double nextGaussian() {
        return random.nextGaussian();
    }

    @Override
    public int nextInt() {
        return random.nextInt();
    }

    @Override
    public long nextLong() {
        return random.nextLong();
    }

    @Override
    public int nextInt(int minInclusive, int maxExclusive) {
        // Unbiased, doesn't lose precision on large ranges like the double-based default.
        // Throws IllegalArgumentException if min >= max.
        return random.nextInt(minInclusive, maxExclusive);
    }

    @Override
    public RandomFork fork() {
        // Consumes one value from this source, so forks are deterministic, but depend on call order.
        return new JavaRandomFork(random.nextLong());
    }

    private record JavaRandomFork(long seed) implements RandomFork {
        @Override
        public RandomSource fromHashOf(String value) {
            return new JavaRandomSource(Seeds.combine(seed, Seeds.hash(value)));
        }

        @Override
        public RandomSource fromSeed(long other) {
            return new JavaRandomSource(Seeds.combine(seed, other));
        }

        @Override
        public RandomSource at(double x, double y) {
            // "+ 0.0" folds -0.0 into 0.0 so both give the same source
            long hx = Double.doubleToLongBits(x + 0.0d);
            long hy = Double.doubleToLongBits(y + 0.0d);
            // Mixed one after the other so (x, y) and (y, x) differ
            return new JavaRandomSource(Seeds.mix(Seeds.mix(seed ^ hx) ^ hy));
        }
    }
}
