package com.seven.procgen.engine.noise.function;

import com.mojang.serialization.MapCodec;
import com.seven.procgen.engine.registry.Registry;

public final class NoiseFunctionTypes {
    public static MapCodec<? extends NoiseFunction> init(Registry<MapCodec<? extends NoiseFunction>> registry) {
        register(registry, "sampler", NoiseFunctions.Sampler.CODEC);
        register(registry, "combiner", NoiseFunctions.Combiner.CODEC);
        register(registry, "unary_function", NoiseFunctions.UnaryFunction.CODEC);
        register(registry, "binary_function", NoiseFunctions.BinaryFunction.CODEC);
        register(registry, "clamp", NoiseFunctions.Clamp.CODEC);
        register(registry, "lerp", NoiseFunctions.Lerp.CODEC);
        register(registry, "affine", NoiseFunctions.Affine.CODEC);
        register(registry, "reference", NoiseFunctions.Reference.CODEC);
        register(registry, "scale", NoiseFunctions.Scale.CODEC);
        register(registry, "offset", NoiseFunctions.Offset.CODEC);
        register(registry, "rotate", NoiseFunctions.Rotate.CODEC);
        register(registry, "domain_warp", NoiseFunctions.DomainWarp.CODEC);
        register(registry, "gradient", NoiseFunctions.Gradient.CODEC);
        register(registry, "radial_gradient", NoiseFunctions.RadialGradient.CODEC);
        register(registry, "terrace", NoiseFunctions.Terrace.CODEC);
        register(registry, "curve_remap", NoiseFunctions.CurveRemap.CODEC);
        register(registry, "select", NoiseFunctions.Select.CODEC);
        return register(registry, "constant", NoiseFunctions.Constant.CODEC);
    }

    private static MapCodec<? extends NoiseFunction> register(Registry<MapCodec<? extends NoiseFunction>> registry, String id, MapCodec<? extends NoiseFunction> codec) {
        return Registry.register(registry, id, codec);
    }
}
