package com.gtnewhorizon.structurelib.commands;

import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.util.Vec3Impl;
import net.minecraft.world.World;

public final class CommandData {
    private static Vec3Impl[] corners = new Vec3Impl[2];

    private static World world = null;

    public static Vec3Impl[] corners() {
        return corners;
    }

    public static void corners(int index, Vec3Impl vec3, World newWorld) {
        corners[index] = vec3;
        world = newWorld;

        if (corners[0] != null && corners[1] != null) {
            drawBoundingBox(new Box(corners[0], corners[1]));
        }
    }

    public static void reset() {
        corners[0] = null;
        corners[1] = null;

        StructureLib.proxy.clearHints(world);

        world = null;
    }

    private static void drawBoundingBox(Box box) {
        StructureLib.proxy.clearHints(world);

        for (int x = box.xMin; x <= box.xMax; x++) {
            StructureLibAPI.hintParticle(world, x, box.yMin, box.zMin, StructureLibAPI.getBlockHint(), 13);
            StructureLibAPI.hintParticle(world, x, box.yMin, box.zMax, StructureLibAPI.getBlockHint(), 13);
            StructureLibAPI.hintParticle(world, x, box.yMax, box.zMin, StructureLibAPI.getBlockHint(), 13);
            StructureLibAPI.hintParticle(world, x, box.yMax, box.zMax, StructureLibAPI.getBlockHint(), 13);
        }

        for (int y = box.yMin; y <= box.yMax; y++) {
            StructureLibAPI.hintParticle(world, box.xMin, y, box.zMin, StructureLibAPI.getBlockHint(), 13);
            StructureLibAPI.hintParticle(world, box.xMin, y, box.zMax, StructureLibAPI.getBlockHint(), 13);
            StructureLibAPI.hintParticle(world, box.xMax, y, box.zMin, StructureLibAPI.getBlockHint(), 13);
            StructureLibAPI.hintParticle(world, box.xMax, y, box.zMax, StructureLibAPI.getBlockHint(), 13);
        }

        for (int z = box.zMin; z <= box.zMax; z++) {
            StructureLibAPI.hintParticle(world, box.xMin, box.yMin, z, StructureLibAPI.getBlockHint(), 13);
            StructureLibAPI.hintParticle(world, box.xMin, box.yMax, z, StructureLibAPI.getBlockHint(), 13);
            StructureLibAPI.hintParticle(world, box.xMax, box.yMin, z, StructureLibAPI.getBlockHint(), 13);
            StructureLibAPI.hintParticle(world, box.xMax, box.yMax, z, StructureLibAPI.getBlockHint(), 13);
        }
    }

    private static class Box {
        public int xMin, xMax;
        public int yMin, yMax;
        public int zMin, zMax;

        Box(Vec3Impl a, Vec3Impl b) {
            if (a.get0() < b.get0()) {
                xMin = a.get0();
                xMax = b.get0();
            } else {
                xMin = b.get0();
                xMax = a.get0();
            }

            if (a.get1() < b.get1()) {
                yMin = a.get1();
                yMax = b.get1();
            } else {
                yMin = b.get1();
                yMax = a.get1();
            }

            if (a.get2() < b.get2()) {
                zMin = a.get2();
                zMax = b.get2();
            } else {
                zMin = b.get2();
                zMax = a.get2();
            }
        }
    }
}
