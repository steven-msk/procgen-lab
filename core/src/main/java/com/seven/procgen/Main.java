package com.seven.procgen;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.seven.procgen.engine.Noise2D;
import com.seven.procgen.engine.WhiteNoise;
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
        Noise2D noise = new WhiteNoise(1234);

        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float v = (float)noise.sample(x, y);
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
