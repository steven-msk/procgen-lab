package com.seven.procgen.engine.noise.function;

import com.mojang.serialization.MapCodec;
import com.seven.procgen.engine.registry.Identifier;
import com.seven.procgen.engine.registry.Registries;
import com.seven.procgen.engine.registry.Registry;
import com.seven.procgen.engine.registry.RegistryKey;

public final class NoiseFunctionTypes {
    public static final RegistryKey<MapCodec<? extends NoiseFunction>> SAMPLER = create("sampler");
    public static final RegistryKey<MapCodec<? extends NoiseFunction>> COMBINER = create("combiner");
    public static final RegistryKey<MapCodec<? extends NoiseFunction>> UNARY_FUNCTION = create("unary_function");
    public static final RegistryKey<MapCodec<? extends NoiseFunction>> BINARY_FUNCTION = create("binary_function");
    public static final RegistryKey<MapCodec<? extends NoiseFunction>> CLAMP = create("clamp");
    public static final RegistryKey<MapCodec<? extends NoiseFunction>> LERP = create("lerp");
    public static final RegistryKey<MapCodec<? extends NoiseFunction>> AFFINE = create("affine");
    public static final RegistryKey<MapCodec<? extends NoiseFunction>> REFERENCE = create("reference");
    public static final RegistryKey<MapCodec<? extends NoiseFunction>> SCALE = create("scale");
    public static final RegistryKey<MapCodec<? extends NoiseFunction>> OFFSET = create("offset");
    public static final RegistryKey<MapCodec<? extends NoiseFunction>> ROTATE = create("rotate");
    public static final RegistryKey<MapCodec<? extends NoiseFunction>> DOMAIN_WARP = create("domain_warp");
    public static final RegistryKey<MapCodec<? extends NoiseFunction>> GRADIENT = create("gradient");
    public static final RegistryKey<MapCodec<? extends NoiseFunction>> RADIAL_GRADIENT = create("radial_gradient");
    public static final RegistryKey<MapCodec<? extends NoiseFunction>> TERRACE = create("terrace");
    public static final RegistryKey<MapCodec<? extends NoiseFunction>> CURVE_REMAP = create("curve_remap");
    public static final RegistryKey<MapCodec<? extends NoiseFunction>> SELECT = create("select");
    public static final RegistryKey<MapCodec<? extends NoiseFunction>> CONSTANT = create("constant");

    public static MapCodec<? extends NoiseFunction> init(Registry<MapCodec<? extends NoiseFunction>> registry) {
        register(registry, SAMPLER, NoiseFunctions.Sampler.CODEC);
        register(registry, COMBINER, NoiseFunctions.Combiner.CODEC);
        register(registry, UNARY_FUNCTION, NoiseFunctions.UnaryFunction.CODEC);
        register(registry, BINARY_FUNCTION, NoiseFunctions.BinaryFunction.CODEC);
        register(registry, CLAMP, NoiseFunctions.Clamp.CODEC);
        register(registry, LERP, NoiseFunctions.Lerp.CODEC);
        register(registry, AFFINE, NoiseFunctions.Affine.CODEC);
        register(registry, REFERENCE, NoiseFunctions.Reference.CODEC);
        register(registry, SCALE, NoiseFunctions.Scale.CODEC);
        register(registry, OFFSET, NoiseFunctions.Offset.CODEC);
        register(registry, ROTATE, NoiseFunctions.Rotate.CODEC);
        register(registry, DOMAIN_WARP, NoiseFunctions.DomainWarp.CODEC);
        register(registry, GRADIENT, NoiseFunctions.Gradient.CODEC);
        register(registry, RADIAL_GRADIENT, NoiseFunctions.RadialGradient.CODEC);
        register(registry, TERRACE, NoiseFunctions.Terrace.CODEC);
        register(registry, CURVE_REMAP, NoiseFunctions.CurveRemap.CODEC);
        register(registry, SELECT, NoiseFunctions.Select.CODEC);
        return register(registry, CONSTANT, NoiseFunctions.Constant.CODEC);
    }

    private static RegistryKey<MapCodec<? extends NoiseFunction>> create(String id) {
        return RegistryKey.of(Registries.NOISE_FUNCTION, Identifier.of(id));
    }

    private static MapCodec<? extends NoiseFunction> register(
        Registry<MapCodec<? extends NoiseFunction>> registry,
        RegistryKey<MapCodec<? extends NoiseFunction>> key,
        MapCodec<? extends NoiseFunction> codec
    ) {
        return Registry.register(registry, key, codec);
    }
}
