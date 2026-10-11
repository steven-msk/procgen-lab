package com.seven.procgen;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.seven.procgen.engine.noise.combiner.NoiseCombiner;
import com.seven.procgen.engine.noise.combiner.NoiseCombinerProvider;
import com.seven.procgen.engine.noise.function.NoiseFunction;
import com.seven.procgen.engine.noise.function.NoiseFunctions;
import com.seven.procgen.engine.noise.sampler.NoiseSamplerProvider;
import com.seven.procgen.engine.noise.sampler.WhiteNoise;
import com.seven.procgen.engine.random.JavaRandomSource;
import com.seven.procgen.engine.random.RandomSource;
import com.seven.procgen.engine.registry.Registry;

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
        int size = 256;
        long seed = 0L;
        RandomSource random = new JavaRandomSource(seed);
//        NoiseFunction noise = NoiseFunctions.createCombiner(FBMCombiner::new, WhiteNoise::new, FBMCombiner.Parameters.of(-1, 1.5d, 1.0d, 0.0d, 0.0d, 0.0d, 1.0d));
//        NoiseFunction noise = NoiseFunctions.createCombiner(RidgedMultifractalCombiner::new, PerlinNoise::new, RidgedMultifractalCombiner.Parameters.of(
//                6, 0.01d, 2.0d, 0.5d, 2.0d, 1.0d
//        ));
        NoiseFunction noise = NoiseFunctions.createSampler(WhiteNoise::new);

        noise = noise.mapAll(new NoiseFunction.Visitor() {
            @Override
            public NoiseFunction apply(NoiseFunction input) {
                return input;
            }

            @Override
            public NoiseFunction.SamplerEntry visitSampler(NoiseFunction.SamplerEntry sampler) {
                NoiseSamplerProvider provider = sampler.provider();
                return new NoiseFunction.SamplerEntry(provider, provider.getSampler(seed));
            }

            @Override
            public <T extends NoiseCombiner.Parameters> NoiseFunction.CombinerEntry<T> visitCombiner(NoiseFunction.CombinerEntry<T> combiner) {
                NoiseCombinerProvider<T> combinerProvider = combiner.provider();
                NoiseSamplerProvider samplerProvider = combiner.samplerProvider();
                T parameters = combiner.parameters();
                return new NoiseFunction.CombinerEntry<>(combinerProvider, samplerProvider, parameters, combinerProvider.getCombiner(random, samplerProvider, parameters));
            }
        });

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
