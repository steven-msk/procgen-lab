package com.seven.procgen;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.seven.procgen.editor.EditorGraph;
import com.seven.procgen.editor.GraphEditor;
import com.seven.procgen.engine.logging.LogUtil;
import com.seven.procgen.engine.noise.combiner.NoiseCombinerType;
import com.seven.procgen.engine.noise.combiner.RidgedMultifractalCombiner;
import com.seven.procgen.engine.noise.data.NoiseState;
import com.seven.procgen.engine.noise.data.SampleField;
import com.seven.procgen.engine.noise.function.NoiseFunction;
import com.seven.procgen.engine.noise.sampler.NoiseSamplerType;
import com.seven.procgen.engine.registry.Registry;
import com.seven.procgen.engine.util.color.ColorRamp;
import com.seven.procgen.engine.util.math.Axis;
import com.seven.procgen.render.NodeImage;
import com.seven.procgen.render.NoiseImageRenderer;
import com.seven.procgen.render.NoiseTreeImage;
import org.slf4j.Logger;

import java.util.Random;

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
    private GraphEditor graphEditor;
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
        // Island placement: tune to image viewport
        double cx = 512.0d, cy = 512.0d, radius = 498.0d;

        // Mountains, same as before, then domain-warped so the ridges meander
        NoiseFunction mountains = createCombiner("mountains", NoiseCombinerType.RIDGED_MULTIFRACTAL, NoiseSamplerType.PERLIN,
            RidgedMultifractalCombiner.Parameters.of(6, 0.01d, 2.0d, 0.5d, 2.0d, 1.0d)
        );
        NoiseFunction detail = createSampler("detail", NoiseSamplerType.PERLIN, 0.1d, 0.1d, 1.0d);
        NoiseFunction ridges = mountains.add(detail.mul(0.2d));

        NoiseFunction warpNoise = createSampler("warp", NoiseSamplerType.PERLIN, 0.01d, 0.01d, 1.0d);
        NoiseFunction warpY = offset(warpNoise, 1731.0d, 947.0d, 0.0d); // decorrelated copy, no second seed
        NoiseFunction warpedRidges = warp(ridges, warpNoise, warpY, 40.0d);

        // Plains: gentle relief, sitting just above sea level, plus stretched + rotated "strata"
        NoiseFunction plains = createSampler("plains", NoiseSamplerType.PERLIN, 0.02d, 0.02d, 1.0d);
        NoiseFunction strata = rotate(scale(createSampler("strata", NoiseSamplerType.PERLIN), 0.03d, 0.006d, 1.0d), 35.0d);
        NoiseFunction lowlands = plains.mul(0.12d).add(strata.mul(0.05d)).add(constant(0.1d));

        // Biome select: a large-scale mask picks mountains vs lowlands, with a soft transition band
        NoiseFunction mask = createSampler("mask", NoiseSamplerType.PERLIN, 0.005d, 0.005d, 1.0d);
        NoiseFunction land = select(mask, 0.05d, 0.2d, warpedRidges, lowlands);

        // Terraces, faded in only on high ground so the coast stays smooth
        NoiseFunction terraced = affine(terrace(remap01(land), 12, 0.35d), 2.0d, -1.0d);
        NoiseFunction highGround = clamp(affine(land, 2.5d, -0.5d), 0.0d, 1.0d); // 0 at 0.2, 1 at 0.6
        NoiseFunction base = lerp(land, terraced, highGround);

        // Height distribution curve: sea level stays at 0, lowlands flatten, peaks steepen
        NoiseFunction shaped = curveRemap(clamp(base, -1.0d, 1.0d), b -> b
            .point(-1.0d, -1.0d)
            .point(-0.3d, -0.6d)
            .point(0.0d, 0.0d)
            .point(0.3d, 0.2d)
            .point(0.6d, 0.5d)
            .point(1.0d, 1.0d)
        );

        // Island falloff (ocean outside the radius) and a slight north-south tilt
        NoiseFunction island = radialGradient(256, 256, radius * 0.2d, radius * 0.8d, 0.25d, -1.0d);
        NoiseFunction tilt = gradient(Axis.Y, cy - radius, cy + radius, 0.08d, -0.08d);

        return clamp(shaped.mul(0.75d).add(island).add(tilt), -1.0d, 1.0d);
    }

    @Override
    public void create() {
        imGuiContextHandle.init();

        this.batch = new SpriteBatch();
        int size = 512;
        Random random = new Random();
        long seed = random.nextLong();

        this.graphEditor = new GraphEditor(Registry.NODE_SPEC, Registry.NOISE_SAMPLER,
            f -> f.mapAll(new NoiseState(seed, Registry.getDefaultLookup())),
            new NoiseTreeImage.SamplerState(NODE_IMAGE_SIZE, 0.0d, 0.0d, (double)size / NODE_IMAGE_SIZE),
            currentConfig()
        );
        graphEditor.setGraph(EditorGraph.importTree(tree(), Registry.getDefaultLookup()));

        NoiseFunction noise = tree().mapAll(new NoiseState(seed, Registry.getDefaultLookup()));
        this.field = SampleField.sampleFunction(noise, size, size, 0.0d, 0.0d, 1.0d);
        this.renderer = new NoiseImageRenderer();
        this.treeImage = new NoiseTreeImage(noise, new NoiseTreeImage.SamplerState(NODE_IMAGE_SIZE, 0.0d, 0.0d, (double)size / NODE_IMAGE_SIZE), currentConfig());
        refresh();
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
            graphEditor.setImageConfig(currentConfig());
        }

        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1f);
        batch.begin();
        batch.draw(renderer.texture(), 20, 20, 512, 512);
        batch.end();

        imGuiContextHandle.start();
        treeImage.draw();
        graphEditor.draw();
        imGuiContextHandle.end();
    }

    @Override
    public void dispose() {
        imGuiContextHandle.dispose();
        treeImage.dispose();
        renderer.dispose();
        this.batch.dispose();
        graphEditor.dispose();
    }

    public static ProcgenLab getInstance() {
        return instance;
    }

    public ApplicationConfig getConfig() {
        return config;
    }
}
