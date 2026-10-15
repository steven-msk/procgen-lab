package com.seven.procgen.render;

import com.badlogic.gdx.graphics.Pixmap;
import com.seven.procgen.engine.noise.function.NoiseFunction;

public interface NoiseRenderer {
    void render(Pixmap pixmap, NoiseFunction function);
}
