package com.seven.procgen.engine.util.color;

/// Maps t in [0, 1] to a packed RGBA8888 color through a precomputed lookup table.
/// Two stops at the same position make a hard edge (e.g. the coastline in TERRAIN).
public final class ColorRamp {
    private static final int LUT_SIZE = 1024;

    public record Stop(float position, int rgb) {
    }

    public static Stop stop(float position, int rgb) {
        return new Stop(position, rgb);
    }

    public static final ColorRamp GRAYSCALE = new ColorRamp(stop(0.0f, 0x000000), stop(1.0f, 0xffffff));

    /// Blue-white-red. For signed noise: the midpoint (value 0) is white.
    public static final ColorRamp DIVERGING = new ColorRamp(
            stop(0.0f, 0x2b4c9b),
            stop(0.5f, 0xf5f5f5),
            stop(1.0f, 0xb2182b));

    /// Assumes input in [-1, 1] mapped to [0, 1], so sea level is at t = 0.5 (noise value 0).
    public static final ColorRamp TERRAIN = new ColorRamp(
            stop(0.00f, 0x0a2350),  // deep water
            stop(0.42f, 0x1c5a9e),
            stop(0.50f, 0x3d8fcf),  // shallows
            stop(0.50f, 0xe3d49a),  // beach (hard edge at the coast)
            stop(0.54f, 0xcdb97a),
            stop(0.57f, 0x6fae4f),  // grass
            stop(0.70f, 0x3f7f3a),  // forest
            stop(0.82f, 0x7a6a58),  // rock
            stop(0.92f, 0xb8b0a8),
            stop(1.00f, 0xffffff)); // snow

    private final int[] lut = new int[LUT_SIZE];

    public ColorRamp(Stop... stops) {
        int n = stops.length;
        if (n < 2) throw new IllegalArgumentException("A ramp needs at least 2 stops");

        float[] pos = new float[n];
        float[] r = new float[n];
        float[] g = new float[n];
        float[] b = new float[n];
        for (int i = 0; i < n; i++) {
            Stop s = stops[i];
            if (s.position() < 0.0f || s.position() > 1.0f) {
                throw new IllegalArgumentException("Stop position out of [0, 1]: " + s.position());
            }
            if (i > 0 && s.position() < pos[i - 1]) {
                throw new IllegalArgumentException("Stops must be in ascending order");
            }
            pos[i] = s.position();
            r[i] = ((s.rgb() >> 16) & 0xff) / 255.0f;
            g[i] = ((s.rgb() >> 8) & 0xff) / 255.0f;
            b[i] = (s.rgb() & 0xff) / 255.0f;
        }

        for (int k = 0; k < LUT_SIZE; k++) {
            lut[k] = colorAt(pos, r, g, b, k / (float)(LUT_SIZE - 1));
        }
    }

    /// @return packed RGBA8888 (the int layout libGDX's Pixmap.drawPixel expects). t is clamped; NaN gives the first color.
    public int sample(double t) {
        if (!(t > 0.0d)) return lut[0];
        if (t >= 1.0d) return lut[LUT_SIZE - 1];
        return lut[(int)(t * (LUT_SIZE - 1) + 0.5d)];
    }

    private static int colorAt(float[] pos, float[] r, float[] g, float[] b, float t) {
        int n = pos.length;
        if (t <= pos[0]) return pack(r[0], g[0], b[0]);

        int i = n - 1;
        while (pos[i] > t) i--; // last stop at or before t, so a duplicate position takes the right-hand color
        if (i == n - 1) return pack(r[i], g[i], b[i]);

        float f = (t - pos[i]) / (pos[i + 1] - pos[i]); // span > 0: pos[i + 1] > t >= pos[i]
        return pack(
                r[i] + (r[i + 1] - r[i]) * f,
                g[i] + (g[i + 1] - g[i]) * f,
                b[i] + (b[i + 1] - b[i]) * f);
    }

    private static int pack(float r, float g, float b) {
        return (Math.round(r * 255.0f) << 24)
                | (Math.round(g * 255.0f) << 16)
                | (Math.round(b * 255.0f) << 8)
                | 0xff;
    }
}
