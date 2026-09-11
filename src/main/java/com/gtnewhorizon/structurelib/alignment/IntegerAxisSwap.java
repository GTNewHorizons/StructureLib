package com.gtnewhorizon.structurelib.alignment;

import static java.lang.Math.abs;

import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;

import org.joml.Vector3i;

import com.gtnewhorizon.structurelib.alignment.enumerable.Direction;
import com.gtnewhorizon.structurelib.coords.CoordinateSystem;
import com.gtnewhorizon.structurelib.coords.Position;
import com.gtnewhorizon.structurelib.util.Vec3Impl;

public class IntegerAxisSwap {

    private final Vec3Impl forFirstAxis;
    private final Vec3Impl forSecondAxis;
    private final Vec3Impl forThirdAxis;

    public IntegerAxisSwap(ForgeDirection forFirstAxis, ForgeDirection forSecondAxis, ForgeDirection forThirdAxis) {
        this.forFirstAxis = Direction.getAxisVector(forFirstAxis);
        this.forSecondAxis = Direction.getAxisVector(forSecondAxis);
        this.forThirdAxis = Direction.getAxisVector(forThirdAxis);
        if (abs(this.forFirstAxis.get0()) + abs(this.forSecondAxis.get0()) + abs(this.forThirdAxis.get0()) != 1
                || abs(this.forFirstAxis.get1()) + abs(this.forSecondAxis.get1()) + abs(this.forThirdAxis.get1()) != 1
                || abs(this.forFirstAxis.get2()) + abs(this.forSecondAxis.get2()) + abs(this.forThirdAxis.get2())
                        != 1) {
            throw new IllegalArgumentException(
                    "Axis are overlapping/missing! " + forFirstAxis
                            .name() + " " + forSecondAxis.name() + " " + forThirdAxis.name());
        }
    }

    public Vec3Impl translate(Vec3Impl point) {
        return new Vec3Impl(
                forFirstAxis.get0() * point.get0() + forFirstAxis.get1() * point.get1()
                        + forFirstAxis.get2() * point.get2(),
                forSecondAxis.get0() * point.get0() + forSecondAxis.get1() * point.get1()
                        + forSecondAxis.get2() * point.get2(),
                forThirdAxis.get0() * point.get0() + forThirdAxis.get1() * point.get1()
                        + forThirdAxis.get2() * point.get2());
    }

    public Vec3Impl inverseTranslate(Vec3Impl point) {
        return new Vec3Impl(
                forFirstAxis.get0() * point.get0() + forSecondAxis.get0() * point.get1()
                        + forThirdAxis.get0() * point.get2(),
                forFirstAxis.get1() * point.get0() + forSecondAxis.get1() * point.get1()
                        + forThirdAxis.get1() * point.get2(),
                forFirstAxis.get2() * point.get0() + forSecondAxis.get2() * point.get1()
                        + forThirdAxis.get2() * point.get2());
    }

    public Vec3 translate(Vec3 point) {
        return Vec3.createVectorHelper(
                forFirstAxis.get0() * point.xCoord + forFirstAxis.get1() * point.yCoord
                        + forFirstAxis.get2() * point.zCoord,
                forSecondAxis.get0() * point.xCoord + forSecondAxis.get1() * point.yCoord
                        + forSecondAxis.get2() * point.zCoord,
                forThirdAxis.get0() * point.xCoord + forThirdAxis.get1() * point.yCoord
                        + forThirdAxis.get2() * point.zCoord);
    }

    public Vec3 inverseTranslate(Vec3 point) {
        return Vec3.createVectorHelper(
                forFirstAxis.get0() * point.xCoord + forSecondAxis.get0() * point.yCoord
                        + forThirdAxis.get0() * point.zCoord,
                forFirstAxis.get1() * point.xCoord + forSecondAxis.get1() * point.yCoord
                        + forThirdAxis.get1() * point.zCoord,
                forFirstAxis.get2() * point.xCoord + forSecondAxis.get2() * point.yCoord
                        + forThirdAxis.get2() * point.zCoord);
    }

    public void translate(int[] point, int[] out) {
        out[0] = forFirstAxis.get0() * point[0] + forFirstAxis.get1() * point[1] + forFirstAxis.get2() * point[2];
        out[1] = forSecondAxis.get0() * point[0] + forSecondAxis.get1() * point[1] + forSecondAxis.get2() * point[2];
        out[2] = forThirdAxis.get0() * point[0] + forThirdAxis.get1() * point[1] + forThirdAxis.get2() * point[2];
    }

    public void inverseTranslate(int[] point, int[] out) {
        out[0] = forFirstAxis.get0() * point[0] + forSecondAxis.get0() * point[1] + forThirdAxis.get0() * point[2];
        out[1] = forFirstAxis.get1() * point[0] + forSecondAxis.get1() * point[1] + forThirdAxis.get1() * point[2];
        out[2] = forFirstAxis.get2() * point[0] + forSecondAxis.get2() * point[1] + forThirdAxis.get2() * point[2];
    }

    public void translate(double[] point, double[] out) {
        out[0] = forFirstAxis.get0() * point[0] + forFirstAxis.get1() * point[1] + forFirstAxis.get2() * point[2];
        out[1] = forSecondAxis.get0() * point[0] + forSecondAxis.get1() * point[1] + forSecondAxis.get2() * point[2];
        out[2] = forThirdAxis.get0() * point[0] + forThirdAxis.get1() * point[1] + forThirdAxis.get2() * point[2];
    }

    public void inverseTranslate(double[] point, double[] out) {
        out[0] = forFirstAxis.get0() * point[0] + forSecondAxis.get0() * point[1] + forThirdAxis.get0() * point[2];
        out[1] = forFirstAxis.get1() * point[0] + forSecondAxis.get1() * point[1] + forThirdAxis.get1() * point[2];
        out[2] = forFirstAxis.get2() * point[0] + forSecondAxis.get2() * point[1] + forThirdAxis.get2() * point[2];
    }

    public Vector3i translate(Vector3i point) {
        return new Vector3i(
                forFirstAxis.get0() * point.x() + forFirstAxis.get1() * point.y() + forFirstAxis.get2() * point.z(),
                forSecondAxis.get0() * point.x() + forSecondAxis.get1() * point.y() + forSecondAxis.get2() * point.z(),
                forThirdAxis.get0() * point.x() + forThirdAxis.get1() * point.y() + forThirdAxis.get2() * point.z());
    }

    public Vector3i inverseTranslate(Vector3i point) {
        return new Vector3i(
                forFirstAxis.get0() * point.x() + forSecondAxis.get0() * point.y() + forThirdAxis.get0() * point.z(),
                forFirstAxis.get1() * point.x() + forSecondAxis.get1() * point.y() + forThirdAxis.get1() * point.z(),
                forFirstAxis.get2() * point.x() + forSecondAxis.get2() * point.y() + forThirdAxis.get2() * point.z());
    }

    public <C extends CoordinateSystem<C, ?>> Position<C> translate(Position<?> point) {
        return new Position<>(
                forFirstAxis.get0() * point.x() + forFirstAxis.get1() * point.y() + forFirstAxis.get2() * point.z(),
                forSecondAxis.get0() * point.x() + forSecondAxis.get1() * point.y() + forSecondAxis.get2() * point.z(),
                forThirdAxis.get0() * point.x() + forThirdAxis.get1() * point.y() + forThirdAxis.get2() * point.z());
    }

    public <C extends CoordinateSystem<C, ?>> Position<C> inverseTranslate(Position<?> point) {
        return new Position<>(
                forFirstAxis.get0() * point.x() + forSecondAxis.get0() * point.y() + forThirdAxis.get0() * point.z(),
                forFirstAxis.get1() * point.x() + forSecondAxis.get1() * point.y() + forThirdAxis.get1() * point.z(),
                forFirstAxis.get2() * point.x() + forSecondAxis.get2() * point.y() + forThirdAxis.get2() * point.z());
    }
}
