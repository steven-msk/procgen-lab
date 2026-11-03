package com.seven.procgen.engine.noise.combiner;

import com.mojang.serialization.Codec;
import com.seven.procgen.engine.noise.graph.ParameterDescriptor;
import com.seven.procgen.engine.registry.Registries;
import com.seven.procgen.engine.registry.Registry;
import com.seven.procgen.engine.registry.RegistryKey;

public interface NoiseCombinerProvider<T extends NoiseCombiner.Parameters> extends NoiseCombinerGetter<T> {
    Codec<NoiseCombinerProvider<? extends NoiseCombiner.Parameters>> CODEC = Registry.NOISE_COMBINER.byNameCodec();
    Codec<RegistryKey<NoiseCombinerProvider<? extends NoiseCombiner.Parameters>>> KEY_CODEC = RegistryKey.codec(Registries.NOISE_COMBINER);

    Codec<T> parametersCodec();

    ParameterDescriptor<T> parameterDescriptor();

    T defaultParameters();
}
