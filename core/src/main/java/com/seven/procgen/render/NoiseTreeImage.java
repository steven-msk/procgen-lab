package com.seven.procgen.render;

import com.badlogic.gdx.utils.Disposable;
import com.seven.procgen.engine.noise.data.NoiseProbes;
import com.seven.procgen.engine.noise.data.SampleField;
import com.seven.procgen.engine.noise.function.NoiseFunction;
import com.seven.procgen.engine.noise.graph.NoiseGraph;
import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Node images for one bound tree. Rebuild it when the tree or seed changes: the cache is keyed by
/// record equality, which ignores the bound sampler instances.
public class NoiseTreeImage implements Disposable {
    private static final float IMAGE_SIZE = 128.0f;
    private final SamplerState samplerState;
    private final Map<NoiseFunction, NodeImage> images = new HashMap<>();
    private final Node root;
    private NodeImage.Config config;
    private int viewVersion;
    private int nextId;

    public NoiseTreeImage(NoiseFunction boundRoot, SamplerState context, NodeImage.Config config) {
        this.samplerState = context;
        this.config = config;
        Map<NoiseFunction, SampleField> fields = NoiseProbes.sampleAll(
            boundRoot, context.size(), context.size(), context.originX(), context.originY(), context.step()
        );
        this.root = build("root", boundRoot, fields);
    }

    public void setImageConfig(NodeImage.Config config) {
        this.config = config;
        viewVersion++;
    }

    public void draw() {
        if (ImGui.begin("Tree image")) drawNode(root);
        ImGui.end();
    }

    private Node build(String role, NoiseFunction function, Map<NoiseFunction, SampleField> fields) {
        NodeImage image = images.computeIfAbsent(function, k -> new NodeImage(k, fields.get(k))); // shared nodes share one image
        List<Node> children = new ArrayList<>();
        for (NoiseGraph.ChildNode child : NoiseGraph.childrenOf(function)) {
            children.add(build(child.role(), child.node(), fields));
        }
        return new Node(nextId++, role, NoiseGraph.labelOf(function), image, children);
    }

    private void drawNode(Node node) {
        ImGui.pushID(node.id());
//        int flags = ImGuiTreeNodeFlags.DefaultOpen | (node.children().isEmpty() ? ImGuiTreeNodeFlags.Leaf : 0);
        int flags = ImGuiTreeNodeFlags.DefaultOpen;
        if (ImGui.treeNodeEx(node.role() + ": " + node.label(), flags)) {
            node.image().update(samplerState, config, viewVersion); // lazy: only sampled when actually visible
            node.image().draw(IMAGE_SIZE);
            node.children.forEach(this::drawNode);
            ImGui.treePop();
        }
        ImGui.popID();
    }

    @Override
    public void dispose() {
        images.values().forEach(NodeImage::dispose);
        images.clear();
    }

    private record Node(int id, String role, String label, NodeImage image, List<Node> children) {
    }

    public record SamplerState(int size, double originX, double originY, double step) {
    }
}
