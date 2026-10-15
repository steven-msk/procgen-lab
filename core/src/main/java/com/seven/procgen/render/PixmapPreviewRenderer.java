package com.seven.procgen.render;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;
import com.seven.procgen.engine.noise.NoiseField;
import com.seven.procgen.engine.util.color.ColorRamp;
import org.jspecify.annotations.Nullable;

/// Owns the Pixmap/Texture and repaints them from a NoiseField.
public class PixmapPreviewRenderer implements Disposable {
    /// RGBA8888 markers used when highlighting range problems.
    public static final int BELOW_RANGE = 0x00ffffff; // cyan
    public static final int ABOVE_RANGE = 0xff00ffff; // magenta
    public static final int NON_FINITE = 0xffff00ff;  // yellow (NaN or infinity)
    private @Nullable Pixmap pixmap;
    private @Nullable Texture texture;

    /// Maps [rangeMin, rangeMax] onto the ramp. With highlight on, values outside the range
    /// are drawn cyan (below) or magenta (above) instead of clamping silently.
    public Texture update(NoiseField field, ColorRamp ramp, double rangeMin, double rangeMax, boolean highlightOutOfRange) {
        ensureSize(field.width(), field.height());
        Pixmap target = this.pixmap;

        double span = rangeMax - rangeMin;
        for (int y = 0; y < field.height(); y++) {
            for (int x = 0; x < field.width(); x++) {
                double v = field.get(x, y);
                int color;
                if (!Double.isFinite(v)) {
                    color = NON_FINITE;
                } else if (highlightOutOfRange && v < rangeMin) {
                    color = BELOW_RANGE;
                } else if (highlightOutOfRange && v > rangeMax) {
                    color = ABOVE_RANGE;
                } else {
                    color = ramp.sample(span > 0.0d ? (v - rangeMin) / span : 0.5d);
                }
                target.drawPixel(x, y, color);
            }
        }

        this.texture.draw(target, 0, 0);
        return this.texture;
    }

    /// Stretches the field's own min/max over the ramp. Good for seeing shape, but hides absolute range problems.
    public Texture updateAutoRange(NoiseField field, ColorRamp ramp) {
        return update(field, ramp, field.min(), field.max(), false);
    }

    public Texture texture() {
        if (texture == null) throw new IllegalStateException("update() has not been called yet");
        return texture;
    }

    private void ensureSize(int width, int height) {
        if (pixmap != null && pixmap.getWidth() == width && pixmap.getHeight() == height) return;
        release();
        pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None); // overwrite pixels, don't blend
        texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    }

    private void release() {
        if (texture != null) texture.dispose();
        if (pixmap != null) pixmap.dispose();
        texture = null;
        pixmap = null;
    }

    @Override
    public void dispose() {
        release();
    }
}
