package com.gtnewhorizon.structurelib.client.renderer;

import net.minecraft.util.Vec3;

import org.joml.Vector3f;

public class MutVec3 extends Vec3 {

    public MutVec3(double x, double y, double z) {
        super(x, y, z);
    }

    public void set(double x, double y, double z) {
        this.xCoord = x;
        this.yCoord = y;
        this.zCoord = z;
    }

    public MutVec3 set(Vector3f vec) {
        this.xCoord = vec.x;
        this.yCoord = vec.y;
        this.zCoord = vec.z;
        return this;
    }

    public MutVec3 add(Vector3f vec) {
        this.xCoord += vec.x;
        this.yCoord += vec.y;
        this.zCoord += vec.z;
        return this;
    }
}
