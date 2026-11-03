package com.seven.procgen.render;

import com.badlogic.gdx.utils.Disposable;
import com.seven.procgen.engine.noise.data.Histogram;
import com.seven.procgen.engine.noise.data.SampleField;
import com.seven.procgen.engine.noise.function.NoiseFunction;
import com.seven.procgen.engine.util.color.ColorRamp;
import imgui.ImGui;
import org.jspecify.annotations.Nullable;

public final class NodeImage implements Disposable {
    private static final int HISTOGRAM_BINS = 128;
    private final NoiseFunction node;
    private final NoiseImageRenderer renderer = new NoiseImageRenderer();
    private @Nullable SampleField field;
    private SampleField.@Nullable Stats stats;
    private @Nullable Histogram histogram;
    private float[] slice = new float[0];
    private double rangeMin;
    private double rangeMax;
    private String statsLine = "";
    private String rangeLine = "";
    private @Nullable String belowLine;
    private @Nullable String aboveLine;
    private int appliedVersion = -1;

    public NodeImage(NoiseFunction node, @Nullable SampleField sampled) {
        this.node = node;
        this.field = sampled;
    }

    void update(NoiseTreeImage.SamplerState s, Config config, int viewVersion) {
        if (field == null) { // fallback: node wasn't reached on the grid (e.g. under a warp)
            field = SampleField.sampleFunction(node, s.size(), s.size(), s.originX(), s.originY(), s.step());
        }
        if (stats == null) {
            stats = SampleField.Stats.of(field);
            slice = row(field, s.size() / 2);
            statsLine = String.format("minInclusive %.3f maxInclusive %.3f mean %.3f std %.3f%s",
                stats.min(), stats.max(), stats.mean(), stats.stdDeviation(),
                stats.nonFinite() > 0 ? " NON-FINITE: " + stats.nonFinite() : ""
            );
        }
        if (appliedVersion == viewVersion) return;
        appliedVersion = viewVersion;

        boolean auto = config.autoRange() && stats.max() > stats.min(); //false for NaN or constant fields
        rangeMin = auto ? stats.min() : config.rangeMin();
        rangeMax = auto ? stats.max() : config.rangeMax();

        renderer.update(field, config.ramp(), rangeMin, rangeMax, config.highlight() && !auto);
        histogram = Histogram.of(field, rangeMin, rangeMax, HISTOGRAM_BINS);
        rangeLine = String.format("range [%.2f, %.2f]", rangeMin, rangeMax);
        belowLine = histogram.below() > 0f ? String.format("%.1f below range", histogram.below() * 100f) : null;
        aboveLine = histogram.above() > 0f ? String.format("%.1f above range", histogram.above() * 100f) : null;
    }

    void draw(float imageSize) {
        ImGui.image(renderer.texture().getTextureObjectHandle(), imageSize, imageSize);
        ImGui.sameLine();
        ImGui.beginGroup();
        float[] bars = histogram.fractions();
        ImGui.plotHistogram("##histogram", bars, bars.length, 0, "", 0.0f, Math.max(histogram.peak(), 1e-6f), 240.0f, 56.0f);
        ImGui.plotLines("##slice", slice, slice.length, 0, "", (float)rangeMin, (float)rangeMax, 240.0f, 56.0f);
        ImGui.text(statsLine);
        ImGui.textDisabled(rangeLine);
        if (belowLine != null) ImGui.textColored(0.0f, 1.0f, 1.0f, 1.0f, belowLine);
        if (aboveLine != null) ImGui.textColored(1.0f, 0.0f, 1.0f, 1.0f, aboveLine);
        ImGui.endGroup();
    }

    private static float[] row(SampleField field, int y) {
        float[] out = new float[field.width()];
        for (int x = 0; x < out.length; x++) {
            double v = field.get(x, y);
            out[x] = Double.isFinite(v) ? (float)v : 0.0f;
        }
        return out;
    }

    @Override
    public void dispose() {
        renderer.dispose();
    }

    public record Config(ColorRamp ramp, boolean autoRange, double rangeMin, double rangeMax, boolean highlight) {
    }
}
