package com.gtnewhorizon.structurelib.coords;

import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkPosition;

import org.joml.Vector3f;
import org.joml.Vector3i;

import com.gtnewhorizon.gtnhlib.blockpos.BlockPos;

/**
 * A {@link BlockPos} with a statically-checked coordinate system.
 */
public class Position<C extends CoordinateSystem<C, ?>> extends BlockPos {

    public Position() {

    }

    public Position(int x, int y, int z) {
        super(x, y, z);
    }

    public Position(ChunkPosition chunkPosition) {
        super(chunkPosition);
    }

    public Position(Vector3i v) {
        super(v.x, v.y, v.z);
    }

    public Position(Vector3f v) {
        super(MathHelper.floor_float(v.x), MathHelper.floor_float(v.y), MathHelper.floor_float(v.z));
    }

    public Position<C> copy() {
        return new Position<>(x, y, z);
    }
}
