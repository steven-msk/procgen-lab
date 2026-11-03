package com.seven.procgen.editor;

import com.seven.procgen.engine.noise.graph.NodeSpec;

import java.util.LinkedHashMap;
import java.util.Map;

public class EditorNode {
    final int id;
    final NodeSpec spec;
    float x, y;
    boolean previewOpen = true;
    final Map<String, Object> params = new LinkedHashMap<>(); // inline values / parameters
    final Map<String, Double> inputValues = new LinkedHashMap<>(); // value of each pin while unlinked
    final Map<String, Integer> inputs = new LinkedHashMap<>(); // input name -> source node id

    EditorNode(int id, NodeSpec spec, float x, float y) {
        this.id = id;
        this.spec = spec;
        this.x = x;
        this.y = y;
        spec.params().forEach(p -> params.put(p.name(), p.defaultFor(id)));
        spec.inputs().forEach(pin -> inputValues.put(pin, spec.inputDefault(pin)));
    }
}
