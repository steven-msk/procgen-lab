package com.seven.procgen.editor;

import com.seven.procgen.engine.noise.graph.ParamSpec;
import com.seven.procgen.render.NodeImage;
import imgui.ImGui;

final class NodeInspector {
    private static final float IMAGE_SIZE = 256.0f;
    private final EditorState state;
    private final ParamWidgets widgets;

    NodeInspector(EditorState state, ParamWidgets widgets) {
        this.state = state;
        this.widgets = widgets;
    }

    void draw() {
        if (ImGui.begin("Inspector")) {
            EditorNode node = state.selectedNode();
            if (node == null) ImGui.textDisabled("Select a node to inspect it.");
            else drawNode(node);
        }
        ImGui.end();
    }

    private void drawNode(EditorNode node) {
        ImGui.pushID(node.id);
        ImGui.text(node.spec.title() + "  (#" + node.id + ")");
        ImGui.textDisabled(node.spec.category().label());
        ImGui.separator();

        String error = state.preview.errorOf(node.id);
        if (error != null) {
            ImGui.textColored(1.0f, 0.35f, 0.35f, 1.0f, "Error");
            ImGui.textWrapped(error);
        } else if (state.preview.isBlocked(node.id)) {
            ImGui.textDisabled("An upstream node has an error, so this node has no value yet.");
        }

        NodeImage image = state.preview.image(node.id);
        if (image != null) image.draw(IMAGE_SIZE);
        else if (error == null) ImGui.textDisabled("No preview.");

        boolean header = false;
        for (ParamSpec<?> param : node.spec.params()) {
            if (widgets.isInline(param)) continue; // edited in the node itself
            if (!header) {
                ImGui.separator();
                ImGui.text("Parameters");
                header = true;
            }
            Object next = widgets.edit(param, node.params.get(param.paramName()));
            if (next != null) state.setParam(node.id, param.paramName(), next);
        }
        ImGui.popID();
    }


}
