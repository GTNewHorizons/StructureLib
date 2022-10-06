package com.gtnewhorizon.structurelib.commands;

import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.util.Box;
import com.gtnewhorizon.structurelib.util.Vec3Impl;
import net.minecraft.world.World;

public final class CommandData {
    private static final Vec3Impl[] corners = new Vec3Impl[2];

    private static Box box = null;

    private static World world = null;

    private static ExtendedFacing facing = null;

    public static Vec3Impl[] corners() {
        return corners;
    }

    public static void corners(int index, Vec3Impl vec3, World newWorld) {
        corners[index] = vec3;
        world = newWorld;

        if (corners[0] != null && corners[1] != null) {
            box = new Box(corners[0], corners[1]);
            box.drawBoundingBox(world);
        }
    }

    public static ExtendedFacing facing() {
        return facing;
    }

    public static void facing(ExtendedFacing newFacing) {
        facing = newFacing;

        if (corners[0] != null && corners[1] != null && box != null) {
            box.drawFace(facing.getDirection(), world);
        }
    }

    public static Box box() {
        return box;
    }

    public static Vec3Impl getBasePos() {
        Vec3Impl basePos = Vec3Impl.NULL_VECTOR;
        switch (facing.getDirection()) {
            case NORTH:
                basePos = new Vec3Impl(box.xMax, box.yMax, box.zMin);
                break;
            case SOUTH:
                basePos = new Vec3Impl(box.xMin, box.yMax, box.zMax);
                break;
            case WEST:
                basePos = new Vec3Impl(box.xMin, box.yMax, box.zMin);
                break;
            case EAST:
                basePos = new Vec3Impl(box.xMax, box.yMax, box.zMax);
                break;
            default:
                break;
        }
        return basePos;
    }

    public static void reset() {
        corners[0] = null;
        corners[1] = null;

        StructureLib.proxy.clearHints(world);

        world = null;
        facing = null;
    }
}
