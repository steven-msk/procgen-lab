package com.seven.procgen.engine;

import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public final class NoiseDeterminism {
    private NoiseDeterminism() {
    }

    /// Fixed sample set: lattice-aligned points, negatives, -0.0, and random points at several magnitudes.
    public static final double[][] POINTS = buildPoints();

    private static double[][] buildPoints() {
        List<double[]> pts = new ArrayList<>();
        for (int x = -2; x <= 2; x++)
            for (int y = -2; y <= 2; y++)
                for (int z = -1; z <= 1; z++)
                    pts.add(new double[]{x, y, z});
        pts.add(new double[]{-0.0d, -0.0d, -0.0d});
        pts.add(new double[]{0.5d, -0.5d, 0.25d});
        pts.add(new double[]{-123.456d, 78.9d, -0.001d});

        Random random = new Random(0xC0FFEEL);
        for (double magnitude : new double[]{1.0d, 10.0d, 1_000.0d, 1_000_000.0d}) {
            for (int i = 0; i < 200; i++) {
                pts.add(new double[]{
                    (random.nextDouble() * 2.0d - 1.0d) * magnitude,
                    (random.nextDouble() * 2.0d - 1.0d) * magnitude,
                    (random.nextDouble() * 2.0d - 1.0d) * Math.min(magnitude, 50.0d)
                });
            }
        }
        return pts.toArray(new double[0][]);
    }

    public static double[] run(Eval eval, int[] order) {
        double[] out = new double[POINTS.length];
        for (int idx : order) {
            double[] p = POINTS[idx];
            out[idx] = eval.at(p[0], p[1], p[2]);
        }
        return out;
    }

    public static double[] runParallel(Eval eval) {
        double[] out = new double[POINTS.length];
        IntStream.range(0, POINTS.length).parallel().forEach(i -> {
            double[] p = POINTS[i];
            out[i] = eval.at(p[0], p[1], p[2]);
        });
        return out;
    }

    public static int[] forward() {
        return IntStream.range(0, POINTS.length).toArray();
    }

    public static int[] reversed() {
        int n = POINTS.length;
        return IntStream.range(0, n).map(i -> n - 1 - i).toArray();
    }

    public static int[] shuffled(long seed) {
        int[] order = forward();
        Random random = new Random(seed);
        for (int i = order.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = order[i];
            order[i] = order[j];
            order[j] = tmp;
        }
        return order;
    }

    public static void assertFinite(String what, double[] values) {
        for (int i = 0; i < values.length; i++) {
            Assertions.assertTrue(
                Double.isFinite(values[i]),
                what + ": non-finite value " + values[i] + " at " + describe(i)
            );
        }
    }

    public static void assertSameBits(String what, double[] expected, double[] actual) {
        Assertions.assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            Assertions.assertEquals(
                Double.doubleToLongBits(expected[i]),
                Double.doubleToLongBits(actual[i]),
                what + ": " + expected[i] + " != " + actual[i] + " at " + describe(i)
            );
        }
    }

    /// `a` and `b` must be two independently constructed evaluators of the same thing
    /// (ideally also fed through different Context implementations).
    /// Checks: finite, repeatable, independent of evaluation order, independent of the instance, thread-safe to read.
    public static void assertDeterministic(Eval a, Eval b) {
        double[] baseline = run(a, forward());
        assertFinite("baseline", baseline);

        assertSameBits("repeat on same instance", baseline, run(a, forward()));
        assertSameBits("reversed order", baseline, run(a, reversed()));
        assertSameBits("fresh instance, shuffled order", baseline, run(b, shuffled(1L)));
        assertSameBits("fresh instance, another shuffle", baseline, run(b, shuffled(2L)));
        assertSameBits("parallel evaluation", baseline, runParallel(a));
    }

    private static String describe(int i) {
        double[] p = POINTS[i];
        return "(" + p[0] + ", " + p[1] + ", " + p[2] + ")";
    }

    @FunctionalInterface
    public interface Eval {
        double at(double x, double y, double z);
    }
}
