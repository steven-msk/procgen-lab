package com.seven.procgen;

import java.util.function.IntSupplier;
import java.util.function.ToIntFunction;

@SuppressWarnings("ClassCanBeRecord")
public final class ApplicationConfig {
    public final Window window;
    public final Graphics graphics;

    public ApplicationConfig(Window window, Graphics graphics) {
        this.window = window;
        this.graphics = graphics;
    }

    public static final class Window {
        public final int width;
        public final int height;
        public final String title;

        public Window(int width, int height, String title) {
            this.width = width;
            this.height = height;
            this.title = title;
        }
    }

    public static final class Graphics {
        public final FPSLimit fpsLimit;
        public final int targetFps;
        public final boolean vsync;
        private final IntSupplier refreshRateSupplier;

        public Graphics(FPSLimit fpsLimit, int targetFps, boolean vsync, IntSupplier refreshRateSupplier) {
            this.fpsLimit = fpsLimit;
            this.targetFps = targetFps;
            this.vsync = vsync;
            this.refreshRateSupplier = refreshRateSupplier;
        }

        public enum FPSLimit {
            VSYNC_EXACT(g -> g.refreshRateSupplier.getAsInt()),
            VSYNC_MATCH_FRACTIONAL(g -> g.refreshRateSupplier.getAsInt() + 1),
            TARGET(g -> g.targetFps),
            NONE(g -> 0);

            private final ToIntFunction<Graphics> function;

            FPSLimit(ToIntFunction<Graphics> function) {
                this.function = function;
            }

            public int apply(Graphics graphics) {
                return function.applyAsInt(graphics);
            }
        }
    }
}
