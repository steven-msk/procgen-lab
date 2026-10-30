package com.seven.procgen.engine.noise.graph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.seven.procgen.engine.noise.function.NoiseFunction;
import com.seven.procgen.engine.noise.function.NoiseFunctions;
import com.seven.procgen.engine.registry.Identifier;

import java.util.*;
import java.util.stream.Collectors;

public record NoiseGraph(Map<Identifier, NoiseFunction> definitions, NoiseFunction root) {
    public static final int FORMAT = 1;
    public static final Codec<NoiseGraph> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.INT.validate(v -> v == FORMAT ? DataResult.success(v) : DataResult.error(() -> "Unsupported format " + v)).fieldOf("format").forGetter(g -> FORMAT),
        Codec.unboundedMap(Identifier.CODEC, NoiseFunction.CODEC).optionalFieldOf("definitions", Map.of()).forGetter(NoiseGraph::definitions),
        NoiseFunction.CODEC.fieldOf("root").forGetter(NoiseGraph::root)
    ).apply(i, (format, definitions, root) -> new NoiseGraph(definitions, root)));
    public static final Codec<NoiseFunction> TREE_CODEC = CODEC.flatXmap(NoiseGraph::resolve, tree -> DataResult.success(NoiseGraph.of(tree)));
    private static final String[] LERP_ROLES = {"from", "to", "t"};
    private static final String[] BINARY_ROLES = {"a", "b"};
    private static final String[] INPUT_ROLES = {"input"};

    public NoiseGraph {
        definitions = Collections.unmodifiableMap(new LinkedHashMap<>(definitions));
    }

    public static NoiseGraph of(NoiseFunction tree) {
        return new Hoister().build(tree);
    }

    private DataResult<NoiseFunction> resolve() {
        try {
            return DataResult.success(new Resolver().resolve(root));
        } catch (NoiseGraphException e) {
            String m = e.getMessage();
            return DataResult.error(() -> m);
        }
    }

    /// Direct children in the order mapChildren visits them
    public static List<ChildNode> childrenOf(NoiseFunction function) {
        List<NoiseFunction> found = new ArrayList<>(3);
        function.mapChildren(c -> {
            found.add(c);
            return c;
        });
        String[] roles = function instanceof NoiseFunctions.Lerp ? LERP_ROLES
            : function instanceof NoiseFunctions.BinaryFunction ? BINARY_ROLES
            : INPUT_ROLES;
        List<ChildNode> children = new ArrayList<>(found.size());
        for (int i = 0; i < found.size(); i++) {
            children.add(new ChildNode(i < roles.length ? roles[i] : "#" + i, found.get(i)));
        }
        return children;
    }

    public static String labelOf(NoiseFunction function) {
        return switch (function) {
            case NoiseFunctions.Sampler s -> s.samplerEntry() + " (Sampler)";
            case NoiseFunctions.Combiner<?> c -> c.combinerEntry() + " (Combiner)";
            case NoiseFunctions.Constant k -> k.value() + " (Constant)";
            case NoiseFunctions.UnaryFunction u -> u.operation().name();
            case NoiseFunctions.BinaryFunction b -> b.operation().name();
            case NoiseFunctions.Clamp c -> "Clamp [" + c.min() + ", " + c.max() + "]";
            case NoiseFunctions.Lerp l -> "Lerp";
            case NoiseFunctions.Affine a -> a.scale() == 0.5d && a.bias() == 0.5d
                ? "Remap01 [-1,1] -> [0,1]"
                : "Affine scale=" + a.scale() + " bias=" + a.bias();
            default -> function.getClass().getSimpleName();
        };
    }

    private static final class Hoister {
        private final Map<NoiseFunction, Integer> uses = new HashMap<>();
        private final Map<NoiseFunction, Identifier> ids = new HashMap<>();
        private final Set<Identifier> taken = new HashSet<>();
        private final Map<Identifier, NoiseFunction> definitions = new LinkedHashMap<>();

        NoiseGraph build(NoiseFunction root) {
            count(root);
            NoiseFunction newRoot = rewrite(root);
            return new NoiseGraph(definitions, newRoot);
        }

        private void count(NoiseFunction n) {
            if (uses.merge(n, 1, Integer::sum) == 1) {
                n.mapChildren(c -> {
                    count(c);
                    return c;
                });
            }
        }

        private boolean shared(NoiseFunction n) {
            return uses.get(n) > 1 && !(n instanceof NoiseFunctions.Constant);
        }

        private NoiseFunction rewrite(NoiseFunction n) {
            if (!shared(n)) return n.mapChildren(this::rewrite);
            Identifier id = ids.get(n);
            if (id == null) {
                id = uniqueId(n);
                ids.put(n, id);
                definitions.put(id, n.mapChildren(this::rewrite));
            }
            return new NoiseFunctions.Reference(id);
        }

        private Identifier uniqueId(NoiseFunction n) {
            String base = n instanceof NoiseFunctions.Sampler s ? s.samplerEntry().id().toString()
                : n instanceof NoiseFunctions.Combiner<?> c ? c.combinerEntry().id().toString()
                : typeName(n);
            Identifier id = Identifier.of(base);
            for (int i = 2; !taken.add(id); i++) {
                id = Identifier.of(base + "_" + i);
            }
            return id;
        }

        private static String typeName(NoiseFunction n) {
            return "composite";
        }
    }

    private final class Resolver {
        private final Map<Identifier, NoiseFunction> done = new HashMap<>();
        private final Deque<Identifier> stack = new ArrayDeque<>();
        private final Map<NoiseFunction, NoiseFunction> canonical = new HashMap<>();

        NoiseFunction resolve(NoiseFunction n) {
            if (n instanceof NoiseFunctions.Reference(Identifier id)) return lookup(id);
            NoiseFunction rebuilt = n.mapChildren(this::resolve);
            return canonical.computeIfAbsent(rebuilt, r -> r); // inline duplicates also collapse
        }

        private NoiseFunction lookup(Identifier id) {
            NoiseFunction cached = done.get(id);
            if (cached != null) return cached;
            if (stack.contains(id))
                throw new NoiseGraphException("Cycle: " + String.join(" -> ", stack.stream().map(Identifier::toString).collect(Collectors.toSet())) + " -> " + id);
            NoiseFunction definition = definitions.get(id);
            if (definition == null) throw new NoiseGraphException("Undefined reference '" + id + "'");

            stack.addLast(id);
            NoiseFunction resolved = resolve(definition);
            stack.removeLast();
            done.put(id, resolved);
            return resolved;
        }
    }

    public record ChildNode(String role, NoiseFunction node) {
    }
}
