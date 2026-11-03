package com.seven.procgen.engine.noise.graph;

import com.google.common.collect.ImmutableSet;
import com.seven.procgen.engine.noise.sampler.NoiseSamplerProvider;
import com.seven.procgen.engine.noise.sampler.NoiseSamplerType;
import com.seven.procgen.engine.registry.Registry;
import com.seven.procgen.engine.registry.RegistryKey;

import java.util.Set;

public class SamplerKeyParam implements ParamSpec<RegistryKey<NoiseSamplerProvider<?>>> {
    public static final SamplerKeyParam INSTANCE = new SamplerKeyParam();
    private static final String NAME = "sampler";

    private SamplerKeyParam() {
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public RegistryKey<NoiseSamplerProvider<?>> defaultValue() {
        return NoiseSamplerType.PERLIN;
    }
}
