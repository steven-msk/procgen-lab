package com.seven.procgen.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.seven.procgen.ApplicationConfig;
import com.seven.procgen.ProcgenLab;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.ValueConverter;

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {
    static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return; // This handles macOS support and helps on Windows.
        createApplication(args);
    }

    private static Lwjgl3Application createApplication(String[] args) {
        ApplicationConfig config = getDefaultApplicationConfig(args);
        Lwjgl3ApplicationConfiguration lwjgl3Config = getDefaultConfiguration(config);
        return new Lwjgl3Application(new ProcgenLab(config), lwjgl3Config);
    }

    private static ApplicationConfig getDefaultApplicationConfig(String[] args) {
        OptionParser parser = new OptionParser();
        OptionSpec<Integer> widthSpec = parser.accepts("width").withRequiredArg().ofType(Integer.class).defaultsTo(Lwjgl3ApplicationConfiguration.getDisplayMode().width);
        OptionSpec<Integer> heightSpec = parser.accepts("height").withRequiredArg().ofType(Integer.class).defaultsTo(Lwjgl3ApplicationConfiguration.getDisplayMode().height);
        OptionSpec<String> titleSpec = parser.accepts("title").withRequiredArg().ofType(String.class).defaultsTo("procgen-lab");
        OptionSpec<Void> vsyncSpec = parser.accepts("vsync");
        OptionSpec<Integer> targetFpsSpec = parser.accepts("targetFps").withRequiredArg().ofType(Integer.class).defaultsTo(0);
        OptionSpec<ApplicationConfig.Graphics.FPSLimit> fpsLimitSpec = parser.accepts("fpsLimit").withRequiredArg().ofType(String.class)
            .withValuesConvertedBy(new ValueConverter<ApplicationConfig.Graphics.FPSLimit>() {
                @Override
                public ApplicationConfig.Graphics.FPSLimit convert(String value) {
                    return ApplicationConfig.Graphics.FPSLimit.valueOf(value);
                }

                @Override
                public Class<? extends ApplicationConfig.Graphics.FPSLimit> valueType() {
                    return ApplicationConfig.Graphics.FPSLimit.class;
                }

                @Override
                public String valuePattern() {
                    return null;
                }
            })
            .defaultsTo(ApplicationConfig.Graphics.FPSLimit.VSYNC_MATCH_FRACTIONAL);
        OptionSet optionSet = parser.parse(args);

        int width = optionSet.valueOf(widthSpec);
        int height = optionSet.valueOf(heightSpec);
        String title = optionSet.valueOf(titleSpec);
        ApplicationConfig.Window window = new ApplicationConfig.Window(width, height, title);

        boolean vsync = optionSet.has(vsyncSpec);
        int targetFps = optionSet.valueOf(targetFpsSpec);
        ApplicationConfig.Graphics.FPSLimit fpsLimit = optionSet.valueOf(fpsLimitSpec);
        ApplicationConfig.Graphics graphics = new ApplicationConfig.Graphics(fpsLimit, targetFps, vsync,
            () -> Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate);

        return new ApplicationConfig(window, graphics);
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration(ApplicationConfig config) {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle(config.window.title);
        //// Vsync limits the frames per second to what your hardware can display, and helps eliminate
        //// screen tearing. This setting doesn't always work on Linux, so the line after is a safeguard.
        configuration.useVsync(config.graphics.vsync);
        //// Limits FPS to the refresh rate of the currently active monitor, plus 1 to try to match fractional
        //// refresh rates. The Vsync setting above should limit the actual FPS to match the monitor.
        configuration.setForegroundFPS(config.graphics.fpsLimit.apply(config.graphics));
        //// If you remove the above line and set Vsync to false, you can get unlimited FPS, which can be
        //// useful for testing performance, but can also be very stressful to some hardware.
        //// You may also need to configure GPU drivers to fully disable Vsync; this can cause screen tearing.

        configuration.setWindowedMode(config.window.width, config.window.height);
        configuration.setMaximized(true);
        //// You can change these files; they are in lwjgl3/src/main/resources/ .
        //// They can also be loaded from the root of assets/ .
        configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");

        //// This could improve compatibility with Windows machines with buggy OpenGL drivers, Macs
        //// with Apple Silicon that have to emulate compatibility with OpenGL anyway, and more.
        //// This uses the dependency `com.badlogicgames.gdx:gdx-lwjgl3-angle` to function.
        //// You would need to add this line to lwjgl3/build.gradle , below the dependency on `gdx-backend-lwjgl3`:
        ////     implementation "com.badlogicgames.gdx:gdx-lwjgl3-angle:$gdxVersion"
        //// You can choose to add the following line and the mentioned dependency if you want; they
        //// are not intended for games that use GL30 (which is compatibility with OpenGL ES 3.0).
        //// Know that it might not work well in some cases.
//        configuration.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.ANGLE_GLES20, 0, 0);

        return configuration;
    }
}