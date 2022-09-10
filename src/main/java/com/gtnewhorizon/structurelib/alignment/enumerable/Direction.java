package com.gtnewhorizon.structurelib.alignment.enumerable;

import com.gtnewhorizon.structurelib.util.Vec3Impl;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraftforge.common.util.ForgeDirection;

public enum Direction {
    DOWN(ForgeDirection.DOWN),
    UP(ForgeDirection.UP),
    NORTH(ForgeDirection.NORTH),
    SOUTH(ForgeDirection.SOUTH),
    WEST(ForgeDirection.WEST),
    EAST(ForgeDirection.EAST);

    private final ForgeDirection forgeDirection;
    private final Vec3Impl axisVector;
    public static final Direction[] VALUES = values();
    private static final Map<Vec3Impl, Direction> reverseMap =
            Arrays.stream(VALUES).collect(Collectors.toMap(d -> d.axisVector, Function.identity()));

    Direction(ForgeDirection forgeDirection) {
        this.forgeDirection = forgeDirection;
        axisVector = Vec3Impl.getFromPool(forgeDirection.offsetX, forgeDirection.offsetY, forgeDirection.offsetZ);
    }

    public ForgeDirection getForgeDirection() {
        return forgeDirection;
    }

    public Vec3Impl getAxisVector() {
        return axisVector;
    }

    public static Vec3Impl getAxisVector(ForgeDirection forgeDirection) {
        return VALUES[forgeDirection.ordinal()].axisVector;
    }

    public static Direction getByAxisVector(Vec3Impl vec3) {
        return reverseMap.get(vec3);
    }
}
