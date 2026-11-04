package com.seven.procgen.editor;

import com.seven.procgen.engine.noise.graph.ParamSpec;
import com.seven.procgen.engine.noise.sampler.NoiseSamplerProvider;
import com.seven.procgen.engine.registry.Registry;
import com.seven.procgen.engine.registry.RegistryKey;
import imgui.ImGui;
import imgui.type.ImString;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

final class ParamWidgets {
    private static final float WIDTH = 110.0f;
    private final List<RegistryKey<NoiseSamplerProvider<?>>> samplerKeys;

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

    @Nullable Object edit(ParamSpec<?> spec, Object value) {
        if (spec instanceof ParamSpec.DoubleSpec d) return editDouble(d, (Double)value);
        if (spec instanceof ParamSpec.IntSpec i) return editInt(i, (Integer)value);
        if (spec instanceof ParamSpec.EnumSpec<?> e) return editEnum(e, (Enum<?>)value);
        if (spec instanceof ParamSpec.StringSpec s) return editString(s.paramName(), (String)value);
        if (spec == ParamSpec.SamplerKey.INSTANCE) return editSamplerKey(spec.paramName(), value);
        if (spec instanceof ParamSpec.DoubleArraySpec a) return editArray(a, (double[])value);
        ImGui.textDisabled(spec.paramName() + ": " + value);
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

    private @Nullable Object editEnum(ParamSpec.EnumSpec<?> spec, Enum<?> current) {
        Enum<?> result = null;
        ImGui.setNextItemWidth(WIDTH);
        if (ImGui.beginCombo(spec.paramName(), current.name())) {
            for (Enum<?> constant : spec.values()) {
                if (ImGui.selectable(constant.name(), constant == current)) {
                    result = constant;
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

    @SuppressWarnings("unchecked")
    private @Nullable Object editSamplerKey(String name, Object value) {
        RegistryKey<NoiseSamplerProvider<?>> current = (RegistryKey<NoiseSamplerProvider<?>>)value;
        RegistryKey<NoiseSamplerProvider<?>> result = null;
        ImGui.setNextItemWidth(WIDTH);
        if (ImGui.beginCombo(name, current.value().toString())) {
            for (RegistryKey<NoiseSamplerProvider<?>> key : samplerKeys) {
                if (ImGui.selectable(key.value().toString(), key.equals(current))) {
                    result = key;
                }
            }
            ImGui.endCombo();
        }
        return result;
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


}
