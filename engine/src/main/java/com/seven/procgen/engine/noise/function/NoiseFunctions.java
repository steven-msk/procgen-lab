package com.seven.procgen.engine.noise.function;

import com.seven.procgen.engine.registry.Registry;
import com.seven.procgen.engine.registry.RegistryEntryLookup;

public final class NoiseFunctions {
    public static final NoiseFunction WHITE = register("white", new WhiteNoise());

    private static NoiseFunction register(String id, NoiseFunction function) {
        return Registry.register(Registry.NOISE_FUNCTION, id, function);
    }

    public static NoiseFunction init(Registry<NoiseFunction> registry, RegistryEntryLookup.RegistryLookup lookup) {
        return WHITE;
    }
}
