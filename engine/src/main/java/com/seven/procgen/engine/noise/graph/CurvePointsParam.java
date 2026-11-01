package com.seven.procgen.engine.noise.graph;

import com.seven.procgen.engine.noise.function.NoiseFunctions;

import java.util.List;

public final class CurvePointsParam implements ParamSpec<NoiseFunctions.CurveRemap.PointList> {
    public static final CurvePointsParam INSTANCE = new CurvePointsParam();
    private static final String NAME = "points";

    private CurvePointsParam() {
    }

    @Override
    public NoiseFunctions.CurveRemap.PointList defaultValue() {
        return new NoiseFunctions.CurveRemap.PointList(List.of(
            NoiseFunctions.CurveRemap.Point.of(-1.0d, -1.0d),
            NoiseFunctions.CurveRemap.Point.of(1.0d, 1.0d))
        );
    }

    @Override
    public String name() {
        return NAME;
    }
}
