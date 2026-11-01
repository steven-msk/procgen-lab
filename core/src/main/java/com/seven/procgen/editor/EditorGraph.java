package com.seven.procgen.editor;

import com.seven.procgen.engine.noise.function.NoiseFunction;
import com.seven.procgen.engine.noise.function.NoiseFunctions;
import com.seven.procgen.engine.noise.graph.FunctionBuildContext;
import com.seven.procgen.engine.noise.graph.NodeSpec;
import com.seven.procgen.engine.noise.graph.NodeSpecs;
import com.seven.procgen.engine.noise.graph.ParamSpec;
import com.seven.procgen.engine.registry.Registries;
import com.seven.procgen.engine.registry.RegistryEntryLookup;
import com.seven.procgen.engine.registry.RegistryKey;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.RecordComponent;
import java.util.*;
import java.util.stream.Collectors;

public final class EditorGraph {
    static final NodeSpec OUTPUT_SPEC = NodeSpec.of("Output", NodeSpec.Category.OUTPUT, List.of("value"), List.of(), c -> c.pin("value"));
    private int nextId = 1;
    final Map<Integer, EditorNode> nodes = new LinkedHashMap<>();
    final int outputId;
    long version;

    public EditorGraph() {
        this.outputId = add(OUTPUT_SPEC, 0, 0).id;
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

    public final class Importer {
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

            RegistryKey<NodeSpec> specKey = NodeSpecs.keyOf(function);
            NodeSpec spec = registryLookup.getOrThrow(Registries.NODE_SPEC).getOrThrow(specKey).value();
            EditorNode node = graph.add(spec, 0, 0);
            made.put(function, node);

            Map<String, NoiseFunction> inputs = decompose(function, spec, node.params);
            inputs.forEach((pin, child) -> graph.connect(importNode(graph, child, made).id, node.id, pin));
            return node;
        }

        /// Fills `params` with the function's parameter values and returns its pin -> child function map.
        /// this is ugly
        @Deprecated(forRemoval = true)
        private static Map<String, NoiseFunction> decompose(NoiseFunction function, NodeSpec spec, Map<String, Object> params) {
            RecordComponent[] components = function.getClass().getRecordComponents();
            if (components == null) throw new IllegalArgumentException(function.getClass() + " is not a record");

            Set<String> paramNames = spec.params().stream().map(ParamSpec::name).collect(Collectors.toSet());
            Map<String, NoiseFunction> inputs = new LinkedHashMap<>();
            for (RecordComponent component : components) {
                String name = snakeCase(component.getName());
                Object value;
                try {
                    value = component.getAccessor().invoke(function);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Can't read " + component, e);
                }
                if (value instanceof NoiseFunction child) {
                    if (!spec.inputs().contains(name)) {
                        throw new IllegalStateException(spec.title() + ": record component '" + component.getName() + "' has no pin '" + name + "'");
                    }
                    inputs.put(name, child);
                } else if (paramNames.contains(name)) {
                    params.put(name, value);
                }
            }
            for (String pin : spec.inputs()) {
                if (!inputs.containsKey(pin))
                    throw new IllegalStateException(spec.title() + ": no record component for pin '" + pin + "'");
            }
            for (String param : paramNames) {
                if (!params.containsKey(param))
                    throw new IllegalStateException(spec.title() + ": no record component for param '" + param + "'");
            }
            return inputs;
        }

        private static String snakeCase(String camel) {
            return camel.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
        }
    }
}