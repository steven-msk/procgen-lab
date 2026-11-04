package com.seven.procgen.editor;

import com.badlogic.gdx.utils.Disposable;
import com.seven.procgen.engine.noise.graph.NodeSpec;
import com.seven.procgen.engine.noise.graph.ParamSpec;
import com.seven.procgen.engine.registry.Registry;
import com.seven.procgen.render.NodeImage;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.extension.nodeditor.NodeEditor;
import imgui.extension.nodeditor.NodeEditorConfig;
import imgui.extension.nodeditor.NodeEditorContext;
import imgui.extension.nodeditor.flag.NodeEditorPinKind;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImLong;
import imgui.type.ImString;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/// The node canvas. The only class that talks to imgui-node-editor.
/// The model is the source of truth and the canvas is rebuilt from it every frame: nodes, then one link per
/// connected input pin. Library events (new link, delete, context menu, selection) are turned into EditorState commands.
/// The library keeps its own copy of node positions, so we push a node's position once when it first appears
/// and copy positions back every frame so layout and saving see where the user dragged things.
final class GraphCanvas implements Disposable {
    private static final float NODE_WIDTH = 190.0f;
    private static final float PREVIEW_SIZE = 190.0f;
    private static final float OUTPUT_SIZE = 400.0f;
    private static final float OUTPUT_MARGIN = 12.0f;
    private static final float VALUE_WIDTH = 70.0f; // inline value of an unlinked input pin
    private static final float ITEM_SPACING = 8.0f; // ImGui's default horizontal item spacing
    private static final String ADD_POPUP = "graph-add-node";
    private static final String NODE_POPUP = "graph-node-menu";
    private static final String CHOICE_POPUP = "graph-param-choice";
    private final EditorState state;
    private final ParamWidgets widgets;
    private final List<NodeSpec> menuSpecs;
    private final NodeEditorContext context;
    private final ImLong idA = new ImLong();
    private final ImLong idB = new ImLong();
    private final ImLong idC = new ImLong();
    private final long[] selection = new long[256];
    private final ImString filter = new ImString(64);
    private float popupX;
    private float popupY;
    private int menuNodeId = EditorState.NONE;
    private ParamWidgets.@Nullable PendingChoice activeChoice;

    GraphCanvas(EditorState state, ParamWidgets widgets, Registry<NodeSpec> specs) {
        this.state = state;
        this.widgets = widgets;
        this.menuSpecs = specs.getEntrySet().stream()
            .map(Map.Entry::getValue)
            .filter(s -> s.category() != NodeSpec.Category.OUTPUT)
            .sorted(Comparator.comparing(NodeSpec::category).thenComparing(NodeSpec::title))
            .toList();
        NodeEditorConfig config = new NodeEditorConfig();
        config.setSettingsFile(null); // don't let the library write its own json next to the jar, we own positions
        this.context = NodeEditor.createEditor(config);
    }

    void draw() {
        if (ImGui.begin("Graph", ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse)) {
            EditorGraph graph = state.graph();
            NodeEditor.setCurrentEditor(context);
            NodeEditor.begin("graph");

            for (EditorNode node : graph.nodes.values()) {
                drawNode(graph, node);
            }
            drawLinks(graph);
            handleCreate(graph);
            handleDelete(graph);
            syncPositions(graph);
            syncSelection();
            if (state.consumeFitRequest()) {
                NodeEditor.navigateToContent(0.0f);
            }
            handleContextMenus(graph);

            NodeEditor.end();
            drawOutputPreview(graph);
        }
        ImGui.end();
    }

    private void drawNode(EditorGraph graph, EditorNode node) {
        long nodeId = PinIds.node(node.id);
        if (state.markPlaced(node.id)) NodeEditor.setNodePosition(nodeId, node.x, node.y);

        NodeSpec spec = node.spec;
        boolean isOutput = node.id == graph.outputId;
        ImGui.pushID(node.id);
        NodeEditor.beginNode(nodeId);

        float[] tint = tint(spec.category());
        ImGui.textColored(tint[0], tint[1], tint[2], 1.0f, spec.title());
        if (!isOutput) {
            ImGui.sameLine();
            if (ImGui.smallButton(node.previewOpen ? "-" : "+")) {
                node.previewOpen = !node.previewOpen;
            }
        }

        String error = state.preview.errorOf(node.id);
        if (error != null) {
            ImGui.textColored(1.0f, 0.35f, 0.35f, 1.0f, "error");
            if (ImGui.isItemHovered()) ImGui.setTooltip(error);
        } else if (state.preview.isBlocked(node.id)) {
            ImGui.textDisabled("blocked upstream");
        }
        ImGui.dummy(NODE_WIDTH, 2.0f); // fixes the node's minimum width

        List<String> pins = spec.inputs();
        int rows = Math.max(pins.size(), isOutput ? 0 : 1); // a node without inputs still gets one row for its out pin
        for (int i = 0; i < rows; i++) {
            float used = 0.0f; // width taken on this row by the input pin
            if (i < pins.size()) {
                String pin = pins.get(i);
                String label = "-> " + pin;
                NodeEditor.beginPin(PinIds.input(node.id, i), NodeEditorPinKind.Input);
                NodeEditor.pinPivotAlignment(0.0f, 0.5f); // links attach to the node's left edge
                NodeEditor.pinPivotSize(0.0f, 0.0f);
                ImGui.text(label);
                NodeEditor.endPin();
                used = ImGui.calcTextSize(label).x;
                if (!node.inputs.containsKey(pin)) { // unlinked: edit the constant the pin falls back to
                    ImGui.sameLine();
                    float[] v = {node.inputValues.get(pin).floatValue()};
                    ImGui.setNextItemWidth(VALUE_WIDTH);
                    if (ImGui.dragFloat("##default_" + pin, v, 0.01f)) {
                        state.setInputValue(node.id, pin, v[0]);
                    }
                    used += ITEM_SPACING + VALUE_WIDTH;
                }
            }
            if (i == 0 && !isOutput) drawOutputPin(node.id, used, i < pins.size());
        }

        boolean hasExtended = false;
        for (ParamSpec<?> param : spec.params()) {
            if (!widgets.isInline(param)) {
                hasExtended = true;
                continue;
            }
            Object next = widgets.edit(param, node.params.get(param.paramName()), node.id, true);
            if (next != null) state.setParam(node.id, param.paramName(), next);
        }
        if (hasExtended) ImGui.textDisabled("more in inspector");

        if (node.previewOpen && !isOutput) { // the Output node has its own large preview in the corner
            NodeImage image = state.preview.image(node.id);
            if (image != null) image.drawImage(PREVIEW_SIZE);
            else ImGui.textDisabled("no preview");
        }

        NodeEditor.endNode();
        ImGui.popID();
    }

    private void drawOutputPin(int nodeId, float used, boolean sharesRow) {
        String label = "out ->";
        float gap = NODE_WIDTH - used - ImGui.calcTextSize(label).x - (sharesRow ? 2.0f * ITEM_SPACING : 0.0f);
        if (sharesRow) ImGui.sameLine();
        ImGui.dummy(Math.max(0.0f, gap), 0.0f);
        ImGui.sameLine();
        NodeEditor.beginPin(PinIds.output(nodeId), NodeEditorPinKind.Output);
        NodeEditor.pinPivotAlignment(1.0f, 0.5f); // links leave from the node's right edge
        NodeEditor.pinPivotSize(0.0f, 0.0f);
        ImGui.text(label);
        NodeEditor.endPin();
    }

    private void drawLinks(EditorGraph graph) {
        for (EditorNode node : graph.nodes.values()) {
            List<String> pins = node.spec.inputs();
            for (int i = 0; i < pins.size(); i++) {
                Integer source = node.inputs.get(pins.get(i));
                if (source == null) continue;
                long inputPin = PinIds.input(node.id, i);
                NodeEditor.link(PinIds.link(inputPin), PinIds.output(source), inputPin);
            }
        }
    }

    private void handleCreate(EditorGraph graph) {
        if (NodeEditor.beginCreate()) {
            if (NodeEditor.queryNewLink(idA, idB)) {
                long start = idA.get();
                long end = idB.get();
                if (PinIds.isPin(start) && PinIds.isPin(end)) { // otherwise the user is still dragging over empty space
                    if (PinIds.isOutput(start) == PinIds.isOutput(end)) {
                        NodeEditor.rejectNewItem();
                    } else {
                        long out = PinIds.isOutput(start) ? start : end;
                        long in = PinIds.isOutput(start) ? end : start;
                        int from = PinIds.pinNode(out);
                        int to = PinIds.pinNode(in);
                        String pin = inputName(graph, in);
                        if (pin == null || !graph.canConnect(from, to, pin)) {
                            NodeEditor.rejectNewItem(); // self link or cycle
                        } else if (NodeEditor.acceptNewItem()) {
                            state.connect(from, to, pin);
                        }
                    }
                }
            }
        }
        NodeEditor.endCreate();
    }

    /// Always-visible preview of the graph's final value, in the bottom right corner of the Graph window.
    /// A child window, so it stays on top of the canvas, moves with the window and gets the mouse where it overlaps.
    private void drawOutputPreview(EditorGraph graph) {
        ImVec2 window = ImGui.getWindowSize();
        float width = OUTPUT_SIZE + 16.0f;
        float height = OUTPUT_SIZE + 40.0f;
        ImGui.setCursorPos(
            Math.max(0.0f, window.x - width - OUTPUT_MARGIN),
            Math.max(0.0f, window.y - height - OUTPUT_MARGIN)
        );
        if (ImGui.beginChild("##output-preview", width, height, true, ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse)) {
            ImGui.textDisabled("Output");
            NodeImage image = state.preview.image(graph.outputId);
            if (image != null) image.drawImage(OUTPUT_SIZE);
            else if (state.preview.isBlocked(graph.outputId)) ImGui.textDisabled("blocked upstream");
            else ImGui.textDisabled("nothing connected");
        }
        ImGui.endChild();
    }


    private void handleDelete(EditorGraph graph) {
        if (NodeEditor.beginDelete()) {
            while (NodeEditor.queryDeletedLink(idA, idB, idC)) {
                if (NodeEditor.acceptDeletedItem()) {
                    long inputPin = PinIds.linkInputPin(idA.get());
                    String pin = inputName(graph, inputPin);
                    if (pin != null) state.disconnect(PinIds.pinNode(inputPin), pin);
                }
            }
            while (NodeEditor.queryDeletedNode(idA)) {
                int id = PinIds.nodeOf(idA.get());
                if (id == graph.outputId) NodeEditor.rejectDeletedItem(); // the Output node can't be removed
                else if (NodeEditor.acceptDeletedItem()) state.removeNode(id);
            }
        }
        NodeEditor.endDelete();
    }

    private static @Nullable String inputName(EditorGraph graph, long inputPin) {
        EditorNode node = graph.nodes.get(PinIds.pinNode(inputPin));
        if (node == null) return null;
        int index = PinIds.inputIndex(inputPin);
        List<String> pins = node.spec.inputs();
        return index >= 0 && index < pins.size() ? pins.get(index) : null;
    }

    private void syncPositions(EditorGraph graph) {
        for (EditorNode node : graph.nodes.values()) {
            long id = PinIds.node(node.id);
            node.x = NodeEditor.getNodePositionX(id);
            node.y = NodeEditor.getNodePositionY(id);
        }
    }

    private void syncSelection() {
        int count = NodeEditor.getSelectedNodes(selection, selection.length);
        state.select(count > 0 ? PinIds.nodeOf(selection[0]) : EditorState.NONE);
    }

    private void handleContextMenus(EditorGraph graph) {
        NodeEditor.suspend(); // let normal ImGui popups work on top of the canvas
        if (NodeEditor.showBackgroundContextMenu()) {
            popupX = NodeEditor.screenToCanvasX(new ImVec2(ImGui.getMousePosX(), 0f));
            popupY = NodeEditor.screenToCanvasY(new ImVec2(0f, ImGui.getMousePosY()));
            filter.set("");
            ImGui.openPopup(ADD_POPUP);
        }
        long contextNode = NodeEditor.getNodeWithContextMenu();
        if (contextNode != -1L) {
            menuNodeId = PinIds.nodeOf(contextNode);
            ImGui.openPopup(NODE_POPUP);
        }
        ParamWidgets.PendingChoice requested = widgets.takePendingChoice(); // a choice button was clicked in a node this frame
        if (requested != null) {
            activeChoice = requested;
            ImGui.openPopup(CHOICE_POPUP);
        }
        drawAddPopup();
        drawNodePopup(graph);
        drawChoicePopup();
        NodeEditor.resume();
    }

    private void drawAddPopup() {
        if (!ImGui.beginPopup(ADD_POPUP)) return;
        if (ImGui.isWindowAppearing()) ImGui.setKeyboardFocusHere();
        ImGui.inputText("##filter", filter);
        String needle = filter.get().toLowerCase(Locale.ROOT);
        NodeSpec.Category last = null;
        for (NodeSpec spec : menuSpecs) {
            if (!needle.isEmpty() && !spec.title().toLowerCase(Locale.ROOT).contains(needle)) continue;
            if (spec.category() != last) {
                last = spec.category();
                ImGui.textDisabled(last.label());
            }
            if (ImGui.selectable(spec.title())) {
                state.addNode(spec, popupX, popupY);
                ImGui.closeCurrentPopup();
            }
        }
        ImGui.endPopup();
    }

    private void drawChoicePopup() {
        if (!ImGui.beginPopup(CHOICE_POPUP)) return;
        ParamWidgets.PendingChoice choice = activeChoice;
        if (choice == null) {
            ImGui.closeCurrentPopup();
        } else {
            for (ParamWidgets.Choice option : choice.choices()) {
                if (ImGui.selectable(option.label(), option.value().equals(choice.current()))) {
                    state.setParam(choice.nodeId(), choice.param(), option.value());
                    ImGui.closeCurrentPopup();
                }
            }
        }
        ImGui.endPopup();
    }

    private void drawNodePopup(EditorGraph graph) {
        if (!ImGui.beginPopup(NODE_POPUP)) return;
        EditorNode node = graph.nodes.get(menuNodeId);
        if (node == null) {
            ImGui.closeCurrentPopup();
        } else {
            if (ImGui.menuItem(node.previewOpen ? "Collapse preview" : "Expand preview")) {
                node.previewOpen = !node.previewOpen;
            }
            if (node.id != graph.outputId && ImGui.menuItem("Delete")) {
                state.removeNode(node.id);
            }
        }
        ImGui.endPopup();
    }

    private static float[] tint(NodeSpec.Category category) {
        return switch (category) {
            case NOISE -> new float[]{0.45f, 0.75f, 1.00f};
            case MATH -> new float[]{0.90f, 0.75f, 0.40f};
            case COORDINATE -> new float[]{0.60f, 0.90f, 0.60f};
            case REMAP -> new float[]{0.85f, 0.60f, 0.90f};
            case OUTPUT -> new float[]{0.80f, 0.80f, 0.80f};
        };
    }

    @Override
    public void dispose() {
        NodeEditor.destroyEditor(context);
    }
}