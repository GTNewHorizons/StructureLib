package com.gtnewhorizon.structurelib.coords;

import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.ChunkPosition;

import org.joml.Vector3f;
import org.joml.Vector3i;

import com.gtnewhorizon.gtnhlib.blockpos.BlockPos;
import com.gtnewhorizon.structurelib.util.Vec3Impl;

/**
 * A {@link BlockPos} with a statically-checked coordinate system.
 */
public class Position<C extends CoordinateSystem<C, ?>> extends BlockPos {

    public Position() {

    }

    public Position(int x, int y, int z) {
        super(x, y, z);
    }

    public Position(float x, float y, float z) {
        super(MathHelper.floor_float(x), MathHelper.floor_float(y), MathHelper.floor_float(z));
    }

    public Position(double x, double y, double z) {
        super(MathHelper.floor_double(x), MathHelper.floor_double(y), MathHelper.floor_double(z));
    }

    public Position(ChunkPosition chunkPosition) {
        super(chunkPosition);
    }

    public Position(Vector3i v) {
        super(v.x, v.y, v.z);
    }

    public Position(Vector3f v) {
        this(v.x, v.y, v.z);
    }

    public Position(Vec3 v) {
        this(v.xCoord, v.yCoord, v.zCoord);
    }

    public Position(Vec3Impl v) {
        this(v.get0(), v.get1(), v.get2());
    }

    public Position<C> copy() {
        return new Position<>(x, y, z);
    }

    public Vector3i toVector3i() {
        return new Vector3i(x, y, z);
    }

    public Vector3f toVector3f() {
        return new Vector3f(x, y, z);
    }

    public Vec3 toVec3() {
        return Vec3.createVectorHelper(x, y, z);
    }

    public Vec3Impl toVec3Impl() {
        return new Vec3Impl(x, y, z);
    }
}
