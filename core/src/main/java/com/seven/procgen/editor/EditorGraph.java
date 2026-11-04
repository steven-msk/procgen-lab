package com.seven.procgen.editor;

import com.seven.procgen.engine.noise.function.NoiseFunction;
import com.seven.procgen.engine.noise.function.NoiseFunctions;
import com.seven.procgen.engine.noise.graph.FunctionBuildContext;
import com.seven.procgen.engine.noise.graph.NodeSpec;
import com.seven.procgen.engine.registry.RegistryEntryLookup;
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

    public static EditorGraph importTree(NoiseFunction function, RegistryEntryLookup.RegistryLookup registryLookup) {
        return new Importer(function, registryLookup).importTree();
    }

    EditorNode add(NodeSpec spec, float x, float y) {
        EditorNode node = new EditorNode(nextId++, spec, x, y);
        nodes.put(node.id, node);
        version++;
        return node;
    }

    /// Used to turn the drag preview red before the drop
    ///
    /// @return true if linking `from` into pin `inputName` of `to` is allowed.
    boolean canConnect(int from, int to, String inputName) {
        EditorNode source = nodes.get(from);
        EditorNode target = nodes.get(to);
        if (source == null || target == null || from == outputId) return false;
        if (!target.spec.inputs().contains(inputName)) return false;
        // dependsOn(from, to): `from` already reads from `to`, so this would be a cycle
        return from != to && !dependsOn(from, to);
    }

    /// Links the output of `from` into pin `inputName` of `to`, replacing any existing link on that pin.
    boolean connect(int from, int to, String inputName) {
        if (!canConnect(from, to, inputName)) return false;
        nodes.get(to).inputs.put(inputName, from);
        version++;
        return true;
    }

    void disconnect(int to, String inputName) {
        EditorNode target = nodes.get(to);
        if (target != null && target.inputs.remove(inputName) != null) {
            version++;
        }
    }

    /// Removes a node and every link read from it.
    /// The Out node can't be removed.
    boolean remove(int id) {
        if (id == outputId || nodes.remove(id) == null) return false;
        nodes.values().forEach(n -> n.inputs.values().removeIf(source -> source == id));
        version++;
        return true;
    }

    /// Replaces a param value. The params are never edited in place (this includes arrays)
    void setParam(int id, String name, Object value) {
        EditorNode node = nodes.get(id);
        if (node == null || !node.params.containsKey(name)) return;
        node.params.put(name, value);
        version++;
    }

    void setInputValue(int id, String pin, double value) {
        EditorNode node = nodes.get(id);
        if (node == null || !node.inputValues.containsKey(pin)) return;
        node.inputValues.put(pin, value);
        version++;
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
        // dangling nodes get a function or an error too
        for (int id : nodes.keySet()) build(id, built, errors, failed);
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
        private final RegistryEntryLookup.RegistryLookup registryLookup;

        private Importer(NoiseFunction root, RegistryEntryLookup.RegistryLookup registryLookup) {
            this.root = root;
            this.registryLookup = registryLookup;
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

            NodeSpec.Entry specEntry = NodeSpec.findMatch(function, registryLookup);
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