package com.seven.procgen.engine.random;

public final class Seeds {
    private Seeds() {
    }

    static long mix(long z) {
        z += 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    static long combine(long a, long b) {
        return mix(a ^ mix(b));
    }

    static long hash(String value) {
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < value.length(); i++) {
            h ^= value.charAt(i);
            h *= 0x100000001b3L;
        }
        return mix(h);
    }
}
