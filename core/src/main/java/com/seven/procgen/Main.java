package com.seven.procgen;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.seven.procgen.engine.logging.LogUtil;
import com.seven.procgen.engine.noise.NoiseField;
import com.seven.procgen.engine.noise.NoiseState;
import com.seven.procgen.engine.noise.combiner.NoiseCombinerType;
import com.seven.procgen.engine.noise.combiner.RidgedMultifractalCombiner;
import com.seven.procgen.engine.noise.function.NoiseFunction;
import com.seven.procgen.engine.noise.sampler.NoiseSamplerType;
import com.seven.procgen.engine.registry.Registry;
import com.seven.procgen.engine.util.color.ColorRamp;
import com.seven.procgen.render.PixmapPreviewRenderer;
import org.slf4j.Logger;

import static com.seven.procgen.engine.noise.function.NoiseFunctions.*;

/**
 * {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms.
 */
public class Main extends ApplicationAdapter {
    private static final Logger LOGGER = LogUtil.GetLogger();
    private PixmapPreviewRenderer preview;
    private NoiseField field;
    private ColorRamp ramp = ColorRamp.TERRAIN;
    private boolean autoRange = false;
    private boolean highlight = true;
    private SpriteBatch batch;

    private NoiseFunction tree() {
        NoiseFunction mountains = createCombiner("mountains", NoiseCombinerType.RIDGED_MULTIFRACTAL, NoiseSamplerType.PERLIN,
                RidgedMultifractalCombiner.Parameters.of(6, 0.01d, 2.0d, 0.5d, 2.0d, 1.0d)
        );
        NoiseFunction detail = createSampler("detail", NoiseSamplerType.PERLIN, 0.1d, 0.1d);
        NoiseFunction plains = createSampler("plains", NoiseSamplerType.PERLIN, 0.02d, 0.02d);

        NoiseFunction a = mountains.add(detail.mul(0.2d));

        NoiseFunction mask = createSampler("mask", NoiseSamplerType.PERLIN, 0.005d, 0.005d);
        NoiseFunction b = lerp(remap01(plains), a, remap01(mask));

        return b;
    }

    @Override
    public void create() {
        Registry.bootstrap();
        this.batch = new SpriteBatch();
        int size = 512;
        long seed = 0L;

        NoiseFunction noise = tree().mapAll(new NoiseState(seed));
        this.field = NoiseField.sample(noise, size, size, 0.0d, 0.0d, 1.0d);
        this.preview = new PixmapPreviewRenderer();
        refresh();
    }

    private void refresh() {
        if (autoRange) preview.updateAutoRange(field, ramp);
        else preview.update(field, ramp, -1.0d, 1.0d, highlight);
    }

    /// Keys 1, 2, and 3 switch the ramp, R toggles auto-range, and H toggles the out-of-range highlight
    @Override
    public void render() {
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
        if (changed) refresh();

        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1f);
        batch.begin();
        batch.draw(preview.texture(), 20, 20, 512, 512);
        batch.end();
    }

    @Override
    public void dispose() {
        this.batch.dispose();
    }
}
