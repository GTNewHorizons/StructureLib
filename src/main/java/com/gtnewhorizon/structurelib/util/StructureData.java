package com.gtnewhorizon.structurelib.util;

import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StructureData {
    private static final Map<UUID, StructureDataEntry> data = new HashMap<>();

    public static StructureDataEntry data(ICommandSender sender) {
        return data(sender.getEntityWorld().getPlayerEntityByName(sender.getCommandSenderName()));
    }

    public static StructureDataEntry data(EntityPlayer player) {
        UUID uuid = player.getUniqueID();

        if (!data.containsKey(uuid)) {
            data.put(uuid, new StructureDataEntry());
        }

        return data.get(uuid);
    }

    public static class StructureDataEntry {
        private final Vec3Impl[] corners = new Vec3Impl[] {null, null};

        private Vec3Impl controller = null;

        private Box box = null;

        private World world = null;

        private ExtendedFacing facing = null;

        public boolean cornersSet() {
            return corners[0] != null && corners[1] != null;
        }

        public Vec3Impl[] corners() {
            return corners;
        }

        public void corners(int index, Vec3Impl vec3, World newWorld) {
            this.corners[index] = vec3;
            this.world = newWorld;

            if (this.cornersSet()) {
                this.box = new Box(corners[0], corners[1]);
                box.drawBoundingBox(this.world);
            }
        }

        public Vec3Impl controller() {
            return controller;
        }

        public void controller(Vec3Impl newController) {
            this.controller = newController;
        }

        public ExtendedFacing facing() {
            return facing;
        }

        public void facing(ExtendedFacing newFacing) {
            facing = newFacing;

            if (this.cornersSet() && box != null) {
                box.drawFace(facing.getDirection(), world);
            }
        }

        public Box box() {
            return box;
        }

        public void box(Box box) {
            this.box = box;
        }

        public void reset() {
            this.corners[0] = null;
            this.corners[1] = null;

            if (this.world != null) {
                StructureLib.proxy.clearHints(world);
            }

            this.world = null;
            this.facing = null;
            this.box = null;
        }

        public boolean isReady() {
            return  box != null &&
                    world != null &&
                    facing != null &&
                    controller != null;
        }
    }
}
