package com.seven.procgen;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.seven.procgen.engine.noise.NoiseState;
import com.seven.procgen.engine.noise.combiner.NoiseCombinerType;
import com.seven.procgen.engine.noise.combiner.RidgedMultifractalCombiner;
import com.seven.procgen.engine.noise.function.NoiseFunction;
import com.seven.procgen.engine.noise.sampler.NoiseSamplerType;
import com.seven.procgen.engine.registry.Registry;

import static com.seven.procgen.engine.noise.function.NoiseFunctions.*;

/**
 * {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms.
 */
public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private Texture texture;

    @Override
    public void create() {
        Registry.bootstrap();
        this.batch = new SpriteBatch();
        int size = 512;
        long seed = 0L;

//        NoiseFunction noise = NoiseFunctions.createCombiner("ridge", NoiseCombinerType.RIDGE_MULTIFRACTAL, NoiseSamplerType.PERLIN, RidgedMultifractalCombiner.Parameters.of(
//                6, 0.01d, 2.0d, 0.5d, 2.0d, 1.0d
//        ));
//        NoiseFunction noise = NoiseFunctions.createSampler(WhiteNoise::new);
//        noise = noise.mapAll(new NoiseState(seed));

        NoiseFunction mountains = createCombiner("mountains", NoiseCombinerType.RIDGED_MULTIFRACTAL, NoiseSamplerType.PERLIN,
                RidgedMultifractalCombiner.Parameters.of(6, 0.01d, 2.0d, 0.5d, 2.0d, 1.0d)
        );
        NoiseFunction detail = createSampler("detail", NoiseSamplerType.PERLIN, 0.1d, 0.1d);
        NoiseFunction plains = createSampler("plains", NoiseSamplerType.PERLIN, 0.02d, 0.02d);

        NoiseFunction a = mountains.add(detail.mul(0.2d));

        NoiseFunction mask = createSampler("mask", NoiseSamplerType.PERLIN, 0.005d, 0.005d);
        NoiseFunction b = lerp(remap01(plains), a, remap01(mask));

        NoiseState state = new NoiseState(seed);
        NoiseFunction noise = b.mapAll(state);

        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float v = Math.abs((float)noise.compute(new NoiseFunction.SinglePointContext(x, y)));
                pixmap.drawPixel(x, y, Color.rgba8888(v, v, v, 1f));
            }
        }
        this.texture = new Texture(pixmap);
        this.texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixmap.dispose();
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1f);
        this.batch.begin();
        this.batch.draw(this.texture, 20, 20, 512, 512);
        this.batch.end();
    }

    @Override
    public void dispose() {
        this.batch.dispose();
        this.texture.dispose();
    }
}
