package com.seven.procgen.engine.registry;

import java.util.Arrays;

public final class Identifier {
    public static final char PATH_SEPARATOR = '/';
    public static final String ALLOWED_CHARS = "-/0123456789_abcdefghijklmnopqrstuvwxyz";
    private static final char[] ALLOWED_CHARS_ARRAY = ALLOWED_CHARS.toCharArray();
    private final String path;

    private Identifier(String path) {
        this.path = path;
        if (!validatePath(path)) throw new InvalidIdentifierException(this);
    }

    public static Identifier of(String path) {
        return new Identifier(path);
    }

    public static Identifier of(String... paths) {
        return new Identifier(String.join(String.valueOf(PATH_SEPARATOR), paths));
    }

    public static boolean validatePath(String path) {
        if (path.isBlank()) return false;

        char[] chars = path.toCharArray();
        for (char c : chars) {
            if (Arrays.binarySearch(ALLOWED_CHARS_ARRAY, c) == -1) {
                return false;
            }
        }
        return true;
    }

    public String getPath() {
        return this.path;
    }

    @Override
    public String toString() {
        return this.path;
    }
}
