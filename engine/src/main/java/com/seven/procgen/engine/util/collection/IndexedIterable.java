package com.seven.procgen.engine.util.collection;

public interface IndexedIterable<T> extends Iterable<T> {
    int ABSENT_RAW_ID = -1;

    T get(int index);

    int getRawId(T value);

    int size();

    default T getOrThrow(int index) {
        T value = this.get(index);
        if (value == null) throw new IndexOutOfBoundsException(index);
        return value;
    }

    default int getRawIdOrThrow(T value) {
        int rawId = this.getRawId(value);
        if (rawId == ABSENT_RAW_ID) throw new IllegalArgumentException("Value not found: " + value);
        return rawId;
    }
}
