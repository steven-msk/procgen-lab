package com.seven.procgen.editor;

import com.seven.procgen.engine.noise.function.NoiseFunction;
import com.seven.procgen.engine.noise.function.NoiseFunctions;
import com.seven.procgen.engine.noise.graph.FunctionBuildContext;
import com.seven.procgen.engine.noise.graph.NodeSpec;
import org.jspecify.annotations.Nullable;

import java.util.*;

public final class EditorGraph {
    static final NodeSpec OUTPUT_SPEC = NodeSpec.of("Output", NodeSpec.Category.OUTPUT, List.of("value"), List.of(), c -> c.pin("value"));
    private int nextId = 1;
    final Map<Integer, EditorNode> nodes = new LinkedHashMap<>();
    final int outputId;
    long version;

    public EditorGraph() {
        this.outputId = add(OUTPUT_SPEC, 0, 0).id;
    }

    public static EditorGraph importTree(NoiseFunction function) {
        return new Importer(function).importTree();
    }

    EditorNode add(NodeSpec spec, float x, float y) {
        EditorNode node = new EditorNode(nextId++, spec, x, y);
        nodes.put(node.id, node);
        version++;
        return node;
    }

    /// Links the output of `from` into pin `inputName` of `to`, replacing any existing link on that pin.
    boolean connect(int from, int to, String inputName) {
        EditorNode source = nodes.get(from);
        EditorNode target = nodes.get(to);
        if (source == null || target == null || from == outputId) return false;
        if (!target.spec.inputs().contains(inputName)) return false;
        if (from == to || dependsOn(from, to)) return false; // would create a cycle
        target.inputs.put(inputName, from);
        version++;
        return true;
    }

    /// True if `node` is `ancestor` or reads from it, directly or through other nodes.
    private boolean dependsOn(int node, int ancestor) {
        Deque<Integer> stack = new ArrayDeque<>();
        Set<Integer> seen = new HashSet<>();
        stack.push(node);
        while (!stack.isEmpty()) {
            int id = stack.pop();
            if (id == ancestor) return true;
            if (seen.add(id)) stack.addAll(nodes.get(id).inputs.values());
        }
        return false;
    }

    Compiled compile() {
        Map<Integer, NoiseFunction> built = new HashMap<>();
        Map<Integer, String> errors = new LinkedHashMap<>();
        Set<Integer> failed = new HashSet<>();
        NoiseFunction root = build(outputId, built, errors, failed);
        return new Compiled(root, built, errors);
    }

    /// Returns null when this node or anything upstream failed. Only the failing node gets an error entry.
    private @Nullable NoiseFunction build(int id, Map<Integer, NoiseFunction> built, Map<Integer, String> errors, Set<Integer> failed) {
        NoiseFunction done = built.get(id);
        if (done != null) return done;
        if (failed.contains(id)) return null;

        EditorNode node = nodes.get(id);
        Map<String, NoiseFunction> pins = new LinkedHashMap<>();
        boolean upstreamOk = true;
        for (String pin : node.spec.inputs()) {
            Integer src = node.inputs.get(pin);
            NoiseFunction f = src != null
                ? build(src, built, errors, failed)
                : NoiseFunctions.constant(node.inputValues.get(pin));
            if (f == null) upstreamOk = false;
            else pins.put(pin, f);
        }
        if (upstreamOk) {
            try {
                NoiseFunction f = node.spec.build(new FunctionBuildContext(pins, node.params));
                built.put(id, f);
                return f;
            } catch (RuntimeException e) {
                errors.put(id, e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            }
        }
        failed.add(id);
        return null;
    }

    /// root is null when the graph can't be built. errors maps node id -> message.
    record Compiled(@Nullable NoiseFunction root, Map<Integer, NoiseFunction> byNode, Map<Integer, String> errors) {
    }

    public static final class Importer {
        private final NoiseFunction root;

        private Importer(NoiseFunction root) {
            this.root = root;
        }

        public EditorGraph importTree() {
            EditorGraph graph = new EditorGraph();
            EditorNode top = importNode(graph, root, new HashMap<>());
            graph.connect(top.id, graph.outputId, "value");
            GraphLayout.layer(graph);
            return graph;
        }

        private EditorNode importNode(EditorGraph graph, NoiseFunction function, Map<NoiseFunction, EditorNode> made) {
            EditorNode existing = made.get(function);
            if (existing != null) return existing;

            NodeSpec.Entry specEntry = NodeSpec.findMatch(function);
            if (specEntry == null) {
                throw new IllegalArgumentException("No node spec for " + function.getClass().getSimpleName());
            }
            EditorNode node = graph.add(specEntry.registryEntry().value(), 0, 0);
            made.put(function, node);
            node.params.putAll(specEntry.decomposed().params());
            specEntry.decomposed().pins().forEach((pin, child) -> graph.connect(importNode(graph, child, made).id, node.id, pin));
            return node;
        }
    }
}