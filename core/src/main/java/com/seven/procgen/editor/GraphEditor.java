package com.seven.procgen.editor;

import com.badlogic.gdx.utils.Disposable;
import com.seven.procgen.engine.noise.data.NoiseFunctionBinder;
import com.seven.procgen.engine.noise.graph.NodeSpec;
import com.seven.procgen.engine.noise.sampler.NoiseSamplerProvider;
import com.seven.procgen.engine.registry.Registry;
import com.seven.procgen.render.NodeImage;
import com.seven.procgen.render.NoiseTreeImage;

/// A node canvas window and an inspector window.
public class GraphEditor implements Disposable {
    private final EditorState state;
    private final GraphCanvas canvas;
    private final NodeInspector inspector;

    public GraphEditor(
        Registry<NodeSpec> specs, Registry<NoiseSamplerProvider<?>> samplers,
        NoiseFunctionBinder binder, NoiseTreeImage.SamplerState sampler, NodeImage.Config imageConfig
    ) {
        this.state = new EditorState(new EditorGraph(), new PreviewCache(binder, sampler, imageConfig));
        ParamWidgets widgets = new ParamWidgets(samplers);
        this.canvas = new GraphCanvas(state, widgets, specs);
        this.inspector = new NodeInspector(state, widgets);
    }

    public EditorGraph graph() {
        return state.graph();
    }

    /// Replaces the edited graph, e.d. with EditorGraph.importTree(...).
    /// The view fits to the new content
    public void setGraph(EditorGraph graph) {
        state.setGraph(graph, true);
    }

    public void setImageConfig(NodeImage.Config config) {
        state.preview.setConfig(config);
    }

    public void setSamplerState(NoiseTreeImage.SamplerState sampler) {
        state.preview.setSampler(sampler);
    }

    public void draw() {
        state.preview.refresh(state.graph());
        canvas.draw();
        inspector.draw();
    }

    @Override
    public void dispose() {
        canvas.dispose();
        state.preview.dispose();
    }
}
