package com.seven.procgen.engine.registry;

import java.util.Objects;

public final class Identifier {
    public static final char PATH_SEPARATOR = '/';
    private final String path;

    private Identifier(String path) {
        this.path = path;
        if (!validatePath(path)) throw new InvalidIdentifierException(this);
    }

    public static Identifier of(String path) {
        Objects.requireNonNull(path, "path");
        return new Identifier(path);
    }

    public static boolean validatePath(String path) {
        Objects.requireNonNull(path, "path");
        if (path.isBlank()) return false;
        String[] split = path.split(String.valueOf(PATH_SEPARATOR), -1);

        for (String s : split) {
            if (s.isBlank()) return false;

            char[] chars = s.toCharArray();
            for (char c : chars) {
                if (!isAllowed(c)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean isAllowed(char c) {
        return (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == PATH_SEPARATOR;
    }

    public String getPath() {
        return this.path;
    }

    @Override
    public String toString() {
        return this.path;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Identifier other && other.path.equals(this.path);
    }

    @Override
    public int hashCode() {
        return this.path.hashCode();
    }
}
