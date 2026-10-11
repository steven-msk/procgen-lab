package com.seven.procgen.engine.random;

public interface RandomSource {
    boolean nextBool();

    /// Must return a value in [0, 1)
    double nextDouble();

    /// Must return a value in [0, 1)
    float nextFloat();

    double nextGaussian();

    int nextInt();

    long nextLong();

    RandomFork fork();

    void setSeed(long seed);

    default int nextInt(int minInclusive, int maxExclusive) {
        double t = this.nextDouble();
        return (int)(minInclusive + (maxExclusive - minInclusive) * t);
    }
}
