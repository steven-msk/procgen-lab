package com.seven.procgen.engine.util.serialization;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.lang.reflect.Array;
import java.util.Arrays;

public final class Codecs {
    public static final Codec<double[]> DOUBLE_ARRAY = Codec.DOUBLE.listOf().xmap(
        list -> list.stream().mapToDouble(Double::doubleValue).toArray(),
        array -> Arrays.stream(array).boxed().toList()
    );

    public static <E extends Enum<E>> Codec<E> enumOf(Class<E> enumClass) {
        return Codec.STRING.comapFlatMap(
            name -> {
                try {
                    return DataResult.success(Enum.valueOf(enumClass, name));
                } catch (IllegalArgumentException e) {
                    return DataResult.error(() -> "Unknown " + enumClass.getSimpleName() + ": " + name);
                }
            }, Enum::name
        );
    }

    @SuppressWarnings("unchecked")
    public static <T> Codec<T[]> arrayOf(Codec<T> elementCodec, Class<T> elementClass) {
        return elementCodec.listOf().xmap(
            list -> list.toArray(size -> (T[])Array.newInstance(elementClass, size)),
            Arrays::asList
        );
    }
}
