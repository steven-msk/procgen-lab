package com.seven.procgen.editor;

import com.seven.procgen.engine.noise.function.NoiseFunction;
import com.seven.procgen.engine.noise.function.NoiseFunctionTypes;
import com.seven.procgen.engine.noise.function.NoiseFunctions;
import com.seven.procgen.engine.noise.graph.NodeSpec;
import com.seven.procgen.engine.noise.graph.NodeSpecs;
import com.seven.procgen.engine.registry.Registry;
import com.seven.procgen.engine.registry.RegistryKey;
import com.seven.procgen.engine.util.math.Axis;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EditorGraphTest {
    static {
        Registry.bootstrap();
    }

    @Test
    void everySpecSurvivesARoundTrip() {
        for (RegistryKey<NodeSpec> key : Registry.NODE_SPEC.getKeys()) {
            EditorGraph graph = new EditorGraph();
            EditorNode node = graph.add(getSpec(key), 0, 0);
            assertTrue(graph.connect(node.id, graph.outputId, "value"), key.valueAsString());

            EditorGraph.Compiled compiled = graph.compile();
            assertTrue(compiled.errors().isEmpty(), key.value() + ": " + compiled.errors());
            assertNotNull(compiled.root(), key.valueAsString());

            EditorGraph back = EditorGraph.importTree(compiled.root(), Registry.getDefaultLookup());
            assertEquals(compiled.root(), back.compile().root(), key.valueAsString());

            long claimants = Registry.NODE_SPEC.getKeys().stream()
                .filter(o -> Registry.NODE_SPEC.getOrThrow(o).value().decompose(compiled.root()) != null)
                .count();
            assertEquals(1, claimants, key.value() + " matched by " + claimants + " specs");
        }
    }

    @Test
    void sharedSubtreesBecomeOneNode() {
        NoiseFunction shared = NoiseFunctions.gradient(Axis.Y, 0, 64, -1, 1);
        NoiseFunction tree = NoiseFunctions.add(NoiseFunctions.abs(shared), shared);
        EditorGraph graph = EditorGraph.importTree(tree, Registry.getDefaultLookup());
        assertEquals(4, graph.nodes.size());   // output, add, abs, gradient
        assertEquals(tree, graph.compile().root());
    }

    @Test
    void cyclesAreRejectedButDiamondsAreFine() {
        EditorGraph g = new EditorGraph();
        EditorNode a = g.add(getSpec(NodeSpecs.getUnaryKey(NoiseFunctions.UnaryOperation.ABS)), 0, 0);
        EditorNode b = g.add(getSpec(NodeSpecs.getUnaryKey(NoiseFunctions.UnaryOperation.NEGATE)), 0, 0);
        EditorNode sum = g.add(getSpec(NodeSpecs.getBinaryKey(NoiseFunctions.BinaryOperation.ADD)), 0, 0);

        assertTrue(g.connect(a.id, b.id, "input"));
        assertFalse(g.connect(b.id, a.id, "input"));   // a -> b -> a
        assertFalse(g.connect(a.id, a.id, "input"));
        assertTrue(g.connect(b.id, sum.id, "a"));
        assertTrue(g.connect(a.id, sum.id, "b"));      // a feeds sum directly and through b
    }

    @Test
    void invalidParamsBecomeNodeErrorsNotCrashes() {
        EditorGraph g = new EditorGraph();
        EditorNode node = g.add(getSpec(NodeSpecs.keyOf(NoiseFunctionTypes.CLAMP)), 0, 0);
        g.connect(node.id, g.outputId, "value");
        node.params.put("min", 5.0d);                  // min > max throws in Clamp's constructor

        EditorGraph.Compiled compiled = g.compile();
        Assertions.assertNull(compiled.root());
        assertTrue(compiled.errors().containsKey(node.id));
    }

    private static NodeSpec getSpec(RegistryKey<NodeSpec> key) {
        return Registry.NODE_SPEC.getOrThrow(key).value();
    }
}
