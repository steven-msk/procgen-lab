package com.seven.procgen.engine.random;

public interface RandomFork {
    RandomSource fromHashOf(String value);

    RandomSource fromSeed(long seed);

    RandomSource at(double x, double y);
}
