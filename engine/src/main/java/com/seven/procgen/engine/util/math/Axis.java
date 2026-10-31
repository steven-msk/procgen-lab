package com.seven.procgen.engine.util.math;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Locale;

public enum Axis {
    X, Y, Z;

    public static final Codec<Axis> CODEC = Codec.STRING.comapFlatMap(s -> {
        try {
            return DataResult.success(valueOf(s.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return DataResult.error(() -> "Unknown axis: " + s);
        }
    }, a -> a.name().toLowerCase(Locale.ROOT));

    public double pick(double x, double y, double z) {
        return switch (this) {
            case X -> x;
            case Y -> y;
            case Z -> z;
        };
    }
}
