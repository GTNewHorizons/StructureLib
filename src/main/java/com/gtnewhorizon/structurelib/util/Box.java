package com.gtnewhorizon.structurelib.util;

import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.StructureLibAPI;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class Box {
    public int xMin, xMax;
    public int yMin, yMax;
    public int zMin, zMax;

    public Box(Vec3Impl a, Vec3Impl b) {
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

    public void drawBoundingBox(World world) {
        StructureLib.proxy.clearHints(world);

        for (int x = this.xMin; x <= this.xMax; x++) {
            StructureLibAPI.hintParticle(world, x, this.yMin, this.zMin, StructureLibAPI.getBlockHint(), 13);
            StructureLibAPI.hintParticle(world, x, this.yMin, this.zMax, StructureLibAPI.getBlockHint(), 13);
            StructureLibAPI.hintParticle(world, x, this.yMax, this.zMin, StructureLibAPI.getBlockHint(), 13);
            StructureLibAPI.hintParticle(world, x, this.yMax, this.zMax, StructureLibAPI.getBlockHint(), 13);
        }

        for (int y = this.yMin; y <= this.yMax; y++) {
            StructureLibAPI.hintParticle(world, this.xMin, y, this.zMin, StructureLibAPI.getBlockHint(), 13);
            StructureLibAPI.hintParticle(world, this.xMin, y, this.zMax, StructureLibAPI.getBlockHint(), 13);
            StructureLibAPI.hintParticle(world, this.xMax, y, this.zMin, StructureLibAPI.getBlockHint(), 13);
            StructureLibAPI.hintParticle(world, this.xMax, y, this.zMax, StructureLibAPI.getBlockHint(), 13);
        }

        for (int z = this.zMin; z <= this.zMax; z++) {
            StructureLibAPI.hintParticle(world, this.xMin, this.yMin, z, StructureLibAPI.getBlockHint(), 13);
            StructureLibAPI.hintParticle(world, this.xMin, this.yMax, z, StructureLibAPI.getBlockHint(), 13);
            StructureLibAPI.hintParticle(world, this.xMax, this.yMin, z, StructureLibAPI.getBlockHint(), 13);
            StructureLibAPI.hintParticle(world, this.xMax, this.yMax, z, StructureLibAPI.getBlockHint(), 13);
        }
    }

    enum RangeVariable {
        X, Y, Z;
    }

    public void drawFace(ForgeDirection face, World world) {
        Vec3Impl cornerA;

        int range1, range2;
        RangeVariable range1Type, range2Type;

//        face = ForgeDirection.UP;

        switch (face) {
            case UP:
                cornerA = new Vec3Impl(this.xMin, this.yMax, this.zMax);

                range1 = this.xMax - this.xMin;
                range1Type = RangeVariable.X;

                range2 = this.zMax - this.zMin;
                range2Type = RangeVariable.Z;
                break;
            case DOWN:
                cornerA = new Vec3Impl(this.xMin, this.yMin, this.zMin);

                range1 = this.xMax - this.xMin;
                range1Type = RangeVariable.X;

                range2 = this.zMax - this.zMin;
                range2Type = RangeVariable.Z;
                break;
            case EAST:
                cornerA = new Vec3Impl(this.xMax, this.yMin, this.zMin);

                range1 = this.yMax - this.yMin;
                range1Type = RangeVariable.Y;

                range2 = this.zMax - this.zMin;
                range2Type = RangeVariable.Z;
                break;
            case WEST:
                cornerA = new Vec3Impl(this.xMin, this.yMin, this.zMin);

                range1 = this.yMax - this.yMin;
                range1Type = RangeVariable.Y;

                range2 = this.zMax - this.zMin;
                range2Type = RangeVariable.Z;
                break;
            case NORTH:
                cornerA = new Vec3Impl(this.xMin, this.yMin, this.zMin);

                range1 = this.yMax - this.yMin;
                range1Type = RangeVariable.Y;

                range2 = this.xMax - this.xMin;
                range2Type = RangeVariable.X;
                break;
            case SOUTH:
                cornerA = new Vec3Impl(this.xMin, this.yMin, this.zMax);

                range1 = this.yMax - this.yMin;
                range1Type = RangeVariable.Y;

                range2 = this.xMax - this.xMin;
                range2Type = RangeVariable.X;
                break;
            default:
                return;
        }

        StructureLibAPI.hintParticleTinted(world, cornerA.get0(), cornerA.get1(), cornerA.get2(), StructureLibAPI.getBlockHint(), 13, new short[] {255, 0, 0});

        for (int var1 = 0; var1 <= range1; var1++) {
            Vec3Impl temp = new Vec3Impl(cornerA.get0(), cornerA.get1(), cornerA.get2());
            for (int var2 = 0; var2 <= range2; var2++) {
                StructureLibAPI.hintParticle(world, cornerA.get0(), cornerA.get1(), cornerA.get2(), StructureLibAPI.getBlockHint(), 13);

                switch (range2Type) {
                    case X:
                        cornerA = cornerA.add(1, 0, 0);
                        break;
                    case Z:
                        cornerA = cornerA.add(0, 0, 1);
                        break;
                }
            }
            switch (range1Type) {
                case X:
                    cornerA = temp.add(1, 0, 0);
                    break;
                case Y:
                    cornerA = temp.add(0, 1, 0);
                    break;
            }
        }
    }
}
