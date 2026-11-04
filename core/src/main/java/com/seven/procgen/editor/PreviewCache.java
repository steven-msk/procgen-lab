package com.seven.procgen.editor;

import com.badlogic.gdx.utils.Disposable;
import com.seven.procgen.engine.noise.data.NoiseFunctionBinder;
import com.seven.procgen.engine.noise.data.NoiseProbes;
import com.seven.procgen.engine.noise.data.SampleField;
import com.seven.procgen.engine.noise.function.NoiseFunction;
import com.seven.procgen.engine.noise.graph.NoiseGraph;
import com.seven.procgen.render.NodeImage;
import com.seven.procgen.render.NoiseTreeImage;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/// Keeps one NodeImage per editor node and refreshes them when the graph changes
/// Sampling and rendering of an individual node only happens when image() is called for it, i.e. when it is visible.
/// Textures survive a refresh (NodeImage.reset), hence dragging a slider doesn't create GL objects.
final class PreviewCache implements Disposable {
    private static final long MIN_INTERVAL_NANOS = 60_000_000L; // at most ~16 builds per second while dragging
    private final NoiseFunctionBinder binder;
    private NoiseTreeImage.SamplerState sampler;
    private NodeImage.Config config;
    private final Map<Integer, NodeImage> images = new HashMap<>();
    private final Map<Integer, String> compileErrors = new HashMap<>();
    private final Map<Integer, String> previewErrors = new HashMap<>();
    private final Set<Integer> blocked = new HashSet<>();
    private @Nullable EditorGraph builtFor;
    private long builtVersion = -1;
    private long lastRun;
    private int viewVersion;

    PreviewCache(NoiseFunctionBinder binder, NoiseTreeImage.SamplerState sampler, NodeImage.Config config) {
        this.binder = binder;
        this.sampler = sampler;
        this.config = config;
    }

    void setConfig(NodeImage.Config config) {
        this.config = config;
        this.viewVersion++;
    }

    void setSampler(NoiseTreeImage.SamplerState sampler) {
        this.sampler = sampler;
        builtVersion = -1;
    }

    /// Call once per frame before drawing.
    /// This remains cheap when nothing changed
    void refresh(EditorGraph graph) {
        boolean sameGraph = graph == builtFor;
        if (sameGraph && graph.version == builtVersion) return;
        long now = System.nanoTime();
        if (sameGraph && now - lastRun < MIN_INTERVAL_NANOS) {
            // still dirty, a later frame will catch up
            return;
        }
        lastRun = now;

        if (!sameGraph) disposeImages(); // node ids restart in a new graph
        builtFor = graph;
        builtVersion = graph.version;
        rebuild(graph);
    }

    private void rebuild(EditorGraph graph) {
        EditorGraph.Compiled compiled = graph.compile();
        compileErrors.clear();
        compileErrors.putAll(compiled.errors());
        previewErrors.clear();
        blocked.clear();

        // one pass over everything under the root, nodes outside of Out upstream aren't in it
        // and are sampled on demand by NodeImage's own fallback
        Map<NoiseFunction, NoiseFunction> bound = new HashMap<>();
        Map<NoiseFunction, SampleField> fields = Map.of();
        if (compiled.root() != null) {
            try {
                NoiseFunction boundRoot = binder.bind(compiled.root());
                indexBound(boundRoot, bound);
                fields = NoiseProbes.sampleAll(
                    boundRoot, sampler.size(), sampler.size(), sampler.originX(), sampler.originY(), sampler.step()
                );
            } catch (RuntimeException e) {
                bound.clear();
                fields = Map.of();
            }
        }

        for (int id : graph.nodes.keySet()) {
            NoiseFunction unbound = compiled.byNode().get(id);
            if (unbound == null) {
                // an upstream node failed
                if (!compileErrors.containsKey(id)) blocked.add(id);
                continue;
            }
            // Records compare equal with or without their bound sampler, so the unbound function finds the bound twin
            NoiseFunction function = bound.get(unbound);
            if (function == null) {
                try {
                    function = binder.bind(unbound);
                } catch (RuntimeException e) {
                    previewErrors.put(id, describe(e));
                    continue;
                }
            }
            SampleField field = fields.get(unbound);
            NodeImage image = images.get(id);
            if (image == null) images.put(id, new NodeImage(function, field));
            else image.reset(function, field);
        }

        images.entrySet().removeIf(e -> {
            if (compiled.byNode().containsKey(e.getKey()) && graph.nodes.containsKey(e.getKey())) {
                return false;
            }
            e.getValue().dispose();
            return true;
        });
    }

    private static void indexBound(NoiseFunction function, Map<NoiseFunction, NoiseFunction> out) {
        if (out.putIfAbsent(function, function) != null) return; // shared subtree already indexed
        for (NoiseGraph.ChildNode child : NoiseGraph.childrenOf(function)) {
            indexBound(child.node(), out);
        }
    }

    /// The node's image, brought up to date for the current config, or null if the node has no value
    /// (it either failed to build or something upstream did, or sampling it threw)
    @Nullable NodeImage image(int nodeId) {
        NodeImage image = images.get(nodeId);
        if (image == null || previewErrors.containsKey(nodeId)) return null;
        try {
            image.update(sampler, config, viewVersion);
            return image;
        } catch (RuntimeException e) {
            previewErrors.put(nodeId, describe(e));
            return null;
        }
    }

    @Nullable String errorOf(int nodeId) {
        String own = compileErrors.get(nodeId);
        return own != null ? own : previewErrors.get(nodeId);
    }

    /// True if the node has no function only because something upstream of it failed
    boolean isBlocked(int nodeId) {
        return blocked.contains(nodeId);
    }

    private static String describe(RuntimeException e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }

    private void disposeImages() {
        images.values().forEach(NodeImage::dispose);
        images.clear();
    }

    @Override
    public void dispose() {
        disposeImages();
    }
}
