package com.seven.procgen.editor;

import java.util.*;

final class GraphLayout {
    private static final float COLUMN_WIDTH = 280.0f;
    private static final float ROW_HEIGHT = 240.0f;

    private GraphLayout() {
    }

    static void layer(EditorGraph graph) {
        Map<Integer, Integer> depth = new HashMap<>();
        List<Integer> visitOrder = new ArrayList<>();
        visit(graph, graph.outputId, 0, depth, visitOrder);

        int maxDepth = Collections.max(depth.values());
        Map<Integer, Integer> rowsUsed = new HashMap<>();
        for (int id : visitOrder) {
            int d = depth.get(id);
            int row = rowsUsed.merge(d, 1, Integer::sum) - 1;
            EditorNode node = graph.nodes.get(id);
            node.x = (maxDepth - d) * COLUMN_WIDTH;
            node.y = row * ROW_HEIGHT;
        }
    }

    private static void visit(EditorGraph graph, int id, int d, Map<Integer, Integer> depth, List<Integer> order) {
        Integer known = depth.get(id);
        if (known != null && known >= d) return;
        if (known == null) order.add(id);
        depth.put(id, d);
        for (int source : graph.nodes.get(id).inputs.values()) {
            visit(graph, source, d + 1, depth, order);
        }
    }
}
