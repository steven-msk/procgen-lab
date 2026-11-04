package com.seven.procgen.editor;

import com.seven.procgen.engine.noise.graph.NodeSpec;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public final class EditorState {
    static final int NONE = -1;
    final PreviewCache preview;
    private EditorGraph graph;
    private int selectedNodeId = NONE;
    /// Nodes whose model pos has already been pushed into imgui-node-editor (which keeps its own copy)
    private final Set<Integer> placed = new HashSet<>();
    private int fitCountdown;

    EditorState(EditorGraph graph, PreviewCache preview) {
        this.graph = graph;
        this.preview = preview;
    }

    EditorGraph graph() {
        return graph;
    }

    void setGraph(EditorGraph graph, boolean fitView) {
        this.graph = graph;
        this.selectedNodeId = NONE;
        this.placed.clear();
        this.fitCountdown = fitView ? 2 : 0; // wait until nodes have been laid out and measured
    }

    @Nullable EditorNode selectedNode() {
        return selectedNodeId == NONE ? null : graph.nodes.get(selectedNodeId);
    }

    void select(int nodeId) {
        this.selectedNodeId = nodeId;
    }

    /// @return true the first time it is called for a node.
    /// The canvas must push the model position into the editor then
    boolean markPlaced(int nodeId) {
        return placed.add(nodeId);
    }

    boolean consumeFitRequest() {
        return fitCountdown > 0 && --fitCountdown == 0;
    }

    EditorNode addNode(NodeSpec spec, float x, float y) {
        return graph.add(spec, x, y);
    }

    void removeNode(int nodeId) {
        if (!graph.remove(nodeId)) return;
        placed.remove(nodeId);
        if (selectedNodeId == nodeId) selectedNodeId = NONE;
    }

    boolean connect(int from, int to, String inputName) {
        return graph.connect(from, to, inputName);
    }

    void disconnect(int to, String inputName) {
        graph.disconnect(to, inputName);
    }

    void setParam(int nodeId, String name, Object value) {
        graph.setParam(nodeId, name, value);
    }

    void setInputValue(int nodeId, String pin, double value) {
        graph.setInputValue(nodeId, pin, value);
    }
}
