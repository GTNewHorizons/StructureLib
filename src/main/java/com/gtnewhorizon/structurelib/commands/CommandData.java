package com.gtnewhorizon.structurelib.commands;

import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.util.Box;
import com.gtnewhorizon.structurelib.util.Vec3Impl;
import net.minecraft.command.ICommandSender;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CommandData {
    private static final Map<UUID, Data> data = new HashMap<>();

    public static Data data(ICommandSender sender) {
        UUID uuid = sender.getEntityWorld().getPlayerEntityByName(sender.getCommandSenderName()).getUniqueID();

        if (!data.containsKey(uuid)) {
            data.put(uuid, new Data());
        }

        return data.get(uuid);
    }

    public static class Data {
        private final Vec3Impl[] corners = new Vec3Impl[2];

        private Vec3Impl controller = null;

        private Box box = null;

        private World world = null;

        private ExtendedFacing facing = null;

        public Vec3Impl[] corners() {
            return corners;
        }

        public void corners(int index, Vec3Impl vec3, World newWorld) {
            this.corners[index] = vec3;
            this.world = newWorld;

            if (corners[0] != null && corners[1] != null) {
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

            if (corners[0] != null && corners[1] != null && box != null) {
                box.drawFace(facing.getDirection(), world);
            }
        }

        public Box box() {
            return box;
        }

        public void reset() {
            this.corners[0] = null;
            this.corners[1] = null;

            StructureLib.proxy.clearHints(world);

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
