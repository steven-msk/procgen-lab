package com.seven.procgen.editor;

import com.seven.procgen.engine.noise.function.NoiseFunctions;
import com.seven.procgen.engine.noise.graph.ParamSpec;
import com.seven.procgen.engine.noise.sampler.NoiseSamplerProvider;
import com.seven.procgen.engine.registry.Registry;
import com.seven.procgen.engine.registry.RegistryKey;
import imgui.ImGui;
import imgui.type.ImString;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

final class ParamWidgets {
    private static final float WIDTH = 110.0f;
    private static final double CURVE_EPSILON = 1.0e-4d;
    private static final int CURVE_SAMPLES = 96;
    private final List<RegistryKey<NoiseSamplerProvider<?>>> samplerKeys;
    private @Nullable PendingChoice pendingChoice;

    ParamWidgets(Registry<NoiseSamplerProvider<?>> samplers) {
        this.samplerKeys = samplers.getEntrySet().stream().map(Map.Entry::getKey).toList();
    }

    /// @return whether the spec is small enough to live inside a node.
    /// Everything else is edited in the inspector
    boolean isInline(ParamSpec<?> spec) {
        return spec instanceof ParamSpec.DoubleSpec
            || spec instanceof ParamSpec.IntSpec
            || spec instanceof ParamSpec.EnumSpec<?>
            || spec instanceof ParamSpec.StringSpec
            || spec == ParamSpec.SamplerKey.INSTANCE;
    }

    @Nullable PendingChoice takePendingChoice() {
        PendingChoice taken = pendingChoice;
        pendingChoice = null;
        return taken;
    }

    /// @param deferChoices true when drawing inside a node on the canvas, false in a normal window.
    @Nullable Object edit(ParamSpec<?> spec, Object value, int nodeId, boolean deferChoices) {
        if (spec instanceof ParamSpec.DoubleSpec d) return editDouble(d, (Double)value);
        if (spec instanceof ParamSpec.IntSpec i) return editInt(i, (Integer)value);
        if (spec instanceof ParamSpec.EnumSpec<?> || spec == ParamSpec.SamplerKey.INSTANCE) {
            return editChoice(spec, value, nodeId, deferChoices);
        }
        if (spec instanceof ParamSpec.StringSpec s) return editString(s.paramName(), (String)value);
        if (spec instanceof ParamSpec.DoubleArraySpec a) return editArray(a, (double[])value);
        if (spec == ParamSpec.CurvePoints.INSTANCE) return editCurve((NoiseFunctions.CurveRemap.PointList)value);
        ImGui.textDisabled(spec.paramName() + ": " + value); // no editor for this kind yet
        return null;
    }


    private @Nullable Object editDouble(ParamSpec.DoubleSpec spec, double current) {
        double min = spec.minInclusive();
        double max = spec.maxInclusive();
        boolean bounded = Math.abs(min) < 1.0e30d && Math.abs(max) < 1.0e30d; // unboundedDouble uses +-Double.MAX_VALUE
        float[] v = {(float)current};
        float speed = bounded ? (float)Math.max((max - min) / 400.0d, 1.0e-4d) : Math.max(Math.abs(v[0]) * 0.01f, 0.01f);
        ImGui.setNextItemWidth(WIDTH);
        boolean changed = bounded
            ? ImGui.dragFloat(spec.paramName(), v, speed, (float)min, (float)max, "%.3f")
            : ImGui.dragFloat(spec.paramName(), v, speed, 0.0f, 0.0f, "%.3f"); // min == max == 0 so ImGui doesn't clamp
        if (!changed) return null;
        return Math.clamp(v[0], min, max); // ctrl+click typing can leave the drag range, so clamp ourselves
    }

    private @Nullable Object editInt(ParamSpec.IntSpec spec, int current) {
        int min = spec.minInclusive();
        int max = spec.maxInclusive();
        boolean bounded = min > Integer.MIN_VALUE && max < Integer.MAX_VALUE;
        int[] v = {current};
        ImGui.setNextItemWidth(WIDTH);
        boolean changed = bounded
            ? ImGui.dragInt(spec.paramName(), v, 0.1f, min, max)
            : ImGui.dragInt(spec.paramName(), v, 0.1f, 0, 0);
        if (!changed) return null;
        return Math.clamp(v[0], min, max);
    }

    private List<Choice> choicesOf(ParamSpec<?> spec, Object current) {
        if (spec == ParamSpec.SamplerKey.INSTANCE) {
            return samplerKeys.stream().map(key -> new Choice(key.value().toString(), key)).toList();
        }
        Enum<?>[] constants = ((Enum<?>)current).getDeclaringClass().getEnumConstants();
        return Arrays.stream(constants).map(constant -> new Choice(constant.name(), constant)).toList();
    }

    private @Nullable Object editChoice(ParamSpec<?> spec, Object current, int nodeId, boolean deferChoices) {
        String name = spec.paramName();
        List<Choice> choices = choicesOf(spec, current);
        String currentLabel = String.valueOf(current);
        for (Choice choice : choices) {
            if (choice.value().equals(current)) currentLabel = choice.label();
        }

        if (deferChoices) {
            if (ImGui.button(currentLabel + "##" + name, WIDTH, 0.0f)) {
                pendingChoice = new PendingChoice(nodeId, name, choices, current);
            }
            ImGui.sameLine();
            ImGui.text(name);
            return null;
        }

        Object result = null;
        ImGui.setNextItemWidth(WIDTH);
        if (ImGui.beginCombo(name, currentLabel)) {
            for (Choice choice : choices) {
                if (ImGui.selectable(choice.label(), choice.value().equals(current))) {
                    result = choice.value();
                }
            }
            ImGui.endCombo();
        }
        return result;
    }


    private @Nullable Object editString(String name, String current) {
        ImString text = new ImString(current, 64);
        ImGui.setNextItemWidth(WIDTH);
        return ImGui.inputText(name, text) ? text.get() : null;
    }

    private @Nullable Object editArray(ParamSpec.DoubleArraySpec spec, double[] current) {
        double[] result = null;
        ImGui.text(spec.paramName());
        for (int i = 0; i < current.length; i++) {
            ImGui.pushID(i);
            float[] v = {(float)current[i]};
            ImGui.setNextItemWidth(WIDTH);
            if (ImGui.dragFloat("##value", v, 0.01f, 0.0f, 0.0f, "%.3f")) {
                result = current.clone();
                result[i] = v[0];
            }
            ImGui.popID();
        }
        if (current.length < spec.maxLength() && ImGui.smallButton("+##add")) {
            result = java.util.Arrays.copyOf(current, current.length + 1);
            result[current.length] = current[current.length - 1];
        }
        if (current.length > spec.minLength()) {
            if (current.length < spec.maxLength()) ImGui.sameLine();
            if (ImGui.smallButton("-##remove")) {
                result = java.util.Arrays.copyOf(current, current.length - 1);
            }
        }
        return result;
    }

    /// Plot of the curve plus one row per point. A PointList needs at least 2 points with strictly increasing `in`,
    /// so `in` is clamped between its neighbors, the last 2 points can't be removed, and a rejected edit is dropped.
    private @Nullable Object editCurve(NoiseFunctions.CurveRemap.PointList list) {
        List<NoiseFunctions.CurveRemap.Point> points = list.points();
        double minIn = list.getFirst().in();
        double maxIn = list.getLast().in();
        double minOut = Double.MAX_VALUE;
        double maxOut = -Double.MAX_VALUE;
        for (NoiseFunctions.CurveRemap.Point p : points) {
            minOut = Math.min(minOut, p.out());
            maxOut = Math.max(maxOut, p.out());
        }
        if (maxOut - minOut < 1.0e-9d) { // give the plot some height if it's too flat
            minOut -= 1.0d;
            maxOut += 1.0d;
        }

        float[] samples = new float[CURVE_SAMPLES];
        for (int i = 0; i < samples.length; i++) {
            samples[i] = (float)list.evaluate(minIn + (maxIn - minIn) * i / (samples.length - 1));
        }
        ImGui.plotLines("##curve", samples, samples.length, 0, "", (float)minOut, (float)maxOut, 240.0f, 90.0f);
        ImGui.textDisabled(String.format("in [%.2f, %.2f]  out [%.2f, %.2f]", minIn, maxIn, minOut, maxOut));

        List<NoiseFunctions.CurveRemap.Point> edited = null;
        ImGui.textDisabled("in");
        ImGui.sameLine(96.0f);
        ImGui.textDisabled("out");
        for (int i = 0; i < points.size(); i++) {
            NoiseFunctions.CurveRemap.Point p = points.get(i);
            double lo = i == 0 ? -Double.MAX_VALUE : points.get(i - 1).in() + CURVE_EPSILON;
            double hi = i == points.size() - 1 ? Double.MAX_VALUE : points.get(i + 1).in() - CURVE_EPSILON;
            float[] in = {(float)p.in()};
            float[] out = {(float)p.out()};

            ImGui.pushID(i);
            ImGui.setNextItemWidth(80.0f);
            boolean inChanged = ImGui.dragFloat("##in", in, 0.01f, 0.0f, 0.0f, "%.3f");
            ImGui.sameLine();
            ImGui.setNextItemWidth(80.0f);
            boolean outChanged = ImGui.dragFloat("##out", out, 0.01f, 0.0f, 0.0f, "%.3f");
            if (inChanged || outChanged) {
                edited = new ArrayList<>(points);
                edited.set(i, NoiseFunctions.CurveRemap.Point.of(
                    inChanged ? Math.clamp(in[0], lo, hi) : p.in(),
                    outChanged ? out[0] : p.out()
                ));
            }
            if (points.size() > 2) {
                ImGui.sameLine();
                if (ImGui.smallButton("x")) {
                    edited = new ArrayList<>(points);
                    edited.remove(i);
                }
            }
            ImGui.popID();
        }

        if (ImGui.smallButton("+ point")) {
            // Split the widest gap in the middle, on the curve, so the shape doesn't change
            int widest = 0;
            double widestGap = -1.0d;
            for (int k = 0; k < points.size() - 1; k++) {
                double gap = points.get(k + 1).in() - points.get(k).in();
                if (gap > widestGap) {
                    widestGap = gap;
                    widest = k;
                }
            }
            double mid = (points.get(widest).in() + points.get(widest + 1).in()) / 2.0d;
            edited = new ArrayList<>(points);
            edited.add(widest + 1, NoiseFunctions.CurveRemap.Point.of(mid, list.evaluate(mid)));
        }

        if (edited == null) return null;
        try {
            return new NoiseFunctions.CurveRemap.PointList(edited);
        } catch (IllegalArgumentException e) {
            return null; // would break the ordering rule (float rounding between very close points)
        }
    }

    record Choice(String label, Object value) {
    }

    /// If a choice button was clicked inside a node, the canvas should open a popup listing `choices`
    record PendingChoice(int nodeId, String param, List<Choice> choices, Object current) {
    }
}
