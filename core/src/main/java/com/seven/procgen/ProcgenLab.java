package com.seven.procgen;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.seven.procgen.engine.logging.LogUtil;
import com.seven.procgen.engine.noise.combiner.NoiseCombinerType;
import com.seven.procgen.engine.noise.combiner.RidgedMultifractalCombiner;
import com.seven.procgen.engine.noise.data.NoiseState;
import com.seven.procgen.engine.noise.data.SampleField;
import com.seven.procgen.engine.noise.function.NoiseFunction;
import com.seven.procgen.engine.noise.graph.NoiseGraph;
import com.seven.procgen.engine.noise.sampler.NoiseSamplerType;
import com.seven.procgen.engine.registry.Registry;
import com.seven.procgen.engine.util.color.ColorRamp;
import com.seven.procgen.render.NodeImage;
import com.seven.procgen.render.NoiseImageRenderer;
import com.seven.procgen.render.NoiseTreeImage;
import org.slf4j.Logger;

import static com.seven.procgen.engine.noise.function.NoiseFunctions.*;

/**
 * {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms.
 */
public class ProcgenLab extends ApplicationAdapter {
    private static final Logger LOGGER = LogUtil.GetLogger();
    private static ProcgenLab instance;
    private final ApplicationConfig config;
    private final ImGuiContextHandle imGuiContextHandle;
    private NoiseImageRenderer renderer;
    private SampleField field;
    private static final int NODE_IMAGE_SIZE = 128;
    private NoiseTreeImage treeImage;
    private ColorRamp ramp = ColorRamp.TERRAIN;
    private boolean autoRange = false;
    private boolean highlight = true;
    private SpriteBatch batch;

    public ProcgenLab(ApplicationConfig config) {
        instance = this;
        this.config = config;
        Registry.bootstrap();
        imGuiContextHandle = new ImGuiContextHandle();
    }

    private NoiseFunction tree() {
        NoiseFunction mountains = createCombiner("mountains", NoiseCombinerType.RIDGED_MULTIFRACTAL, NoiseSamplerType.PERLIN,
            RidgedMultifractalCombiner.Parameters.of(6, 0.01d, 2.0d, 0.5d, 2.0d, 1.0d)
        );
        NoiseFunction detail = createSampler("detail", NoiseSamplerType.PERLIN, 0.1d, 0.1d, 1.0d);
        NoiseFunction plains = createSampler("plains", NoiseSamplerType.PERLIN, 0.02d, 0.02d, 1.0d);

        NoiseFunction a = mountains.add(detail.mul(0.2d));

        NoiseFunction mask = createSampler("mask", NoiseSamplerType.PERLIN, 0.005d, 0.005d, 1.0d);
        NoiseFunction b = lerp(remap01(plains), a, remap01(mask));
        return b;
    }

    @Override
    public void create() {
        imGuiContextHandle.init();

        this.batch = new SpriteBatch();
        int size = 512;
        long seed = 0L;

        NoiseFunction noise = tree().mapAll(new NoiseState(seed, Registry.getDefaultLookup()));
        this.field = SampleField.sampleFunction(noise, size, size, 0.0d, 0.0d, 1.0d);
        this.renderer = new NoiseImageRenderer();
        this.treeImage = new NoiseTreeImage(noise, new NoiseTreeImage.SamplerContext(NODE_IMAGE_SIZE, 0.0d, 0.0d, (double)size / NODE_IMAGE_SIZE), currentConfig());
        refresh();

        JsonElement json = NoiseGraph.TREE_CODEC.encodeStart(JsonOps.INSTANCE, noise).getOrThrow();
        LOGGER.info(new GsonBuilder().setPrettyPrinting().create().toJson(json));
    }

    private NodeImage.Config currentConfig() {
        return new NodeImage.Config(ramp, autoRange, -1.0d, 1.0d, highlight);
    }

    private void refresh() {
        if (autoRange) renderer.updateAutoRange(field, ramp);
        else renderer.update(field, ramp, -1.0d, 1.0d, highlight);
    }

    @Override
    public void render() {
        // Keys 1, 2, and 3 switch the ramp, R toggles auto-range, and H toggles the out-of-range highlight
        boolean changed = false;
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            ramp = ColorRamp.GRAYSCALE;
            LOGGER.info("ramp set to GRAYSCALE");
            changed = true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            ramp = ColorRamp.DIVERGING;
            LOGGER.info("ramp set to DIVERGING");
            changed = true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
            ramp = ColorRamp.TERRAIN;
            LOGGER.info("ramp set to TERRAIN");
            changed = true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            autoRange = !autoRange;
            LOGGER.info("autoRange set to {}", autoRange);
            changed = true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.H)) {
            highlight = !highlight;
            LOGGER.info("highlight set to {}", highlight);
            changed = true;
        }
        if (changed) {
            refresh();
            treeImage.setImageConfig(currentConfig());
        }

        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1f);
        batch.begin();
        batch.draw(renderer.texture(), 20, 20, 512, 512);
        batch.end();

        imGuiContextHandle.start();
        treeImage.draw();
        imGuiContextHandle.end();
    }

    @Override
    public void dispose() {
        imGuiContextHandle.dispose();
        treeImage.dispose();
        renderer.dispose();
        this.batch.dispose();
    }

    public static ProcgenLab getInstance() {
        return instance;
    }

    public ApplicationConfig getConfig() {
        return config;
    }
}
