package com.seven.procgen.engine.noise.data;

import com.seven.procgen.engine.noise.function.NoiseFunction;

import java.util.HashMap;
import java.util.Map;

public final class NoiseProbes {
    private NoiseProbes() {
    }

    public static Map<NoiseFunction, SampleField> sampleAll(NoiseFunction boundRoot, int width, int height, double originX, double originY, double step) {
        int size = width * height;
        Map<NoiseFunction, GridContext.Probe> probes = new HashMap<>();
        NoiseFunction captured = captureProbe(boundRoot, probes, size);

        GridContext context = new GridContext(0.0d);
        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                captured.compute(context.set(index++, originX + x * step, originY + y * step));
            }
        }

        Map<NoiseFunction, SampleField> fields = new HashMap<>();
        for (Map.Entry<NoiseFunction, GridContext.Probe> e : probes.entrySet()) {
            GridContext.Probe probe = e.getValue();
            if (probe.isComplete()) {
                fields.put(e.getKey(), SampleField.direct(width, height, probe.values()));
            }
        }
        return fields;
    }

    private static NoiseFunction captureProbe(NoiseFunction node, Map<NoiseFunction, GridContext.Probe> probes, int size) {
        GridContext.Probe existing = probes.get(node);
        if (existing != null) return existing;
        // get/put instead of computeIfAbsent
        // this recurses, and HashMap forbids modification inside its lambda
        GridContext.Probe probe = new GridContext.Probe(node.mapChildren(child -> captureProbe(child, probes, size)), size);
        probes.put(node, probe);
        return probe;
    }
}
