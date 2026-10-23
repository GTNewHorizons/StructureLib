package com.gtnewhorizon.structurelib.dismantle;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.gtnewhorizon.structurelib.ConfigurationHandler;
import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.alignment.constructable.ConstructableUtility;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.structure.StructureCapture;
import com.gtnewhorizon.structurelib.structure.StructureCapture.Candidate;
import com.gtnewhorizon.structurelib.structure.StructureCapture.Position;

/**
 * Experimental wall-sharing guard. A block is only protected when another loaded, formed GT
 * controller's structure definition positively recognizes that exact world position. Unformed
 * controllers are deliberately ignored: their blueprints do not establish ownership of casings.
 */
final class SharedBlockProtection {

    private SharedBlockProtection() {}

    /** @return the protected positions, or null if a nearby formed machine cannot be scanned safely. */
    static Set<Position> findShared(World world, TileEntity primaryController, StructureCapture primary,
            ItemStack trigger) {
        Set<Position> selected = new LinkedHashSet<>();
        int minX = primaryController.xCoord, minY = primaryController.yCoord, minZ = primaryController.zCoord;
        int maxX = minX, maxY = minY, maxZ = minZ;
        for (Candidate c : primary.getCandidates()) {
            Position p = c.pos;
            selected.add(p);
            minX = Math.min(minX, p.x);
            minY = Math.min(minY, p.y);
            minZ = Math.min(minZ, p.z);
            maxX = Math.max(maxX, p.x);
            maxY = Math.max(maxY, p.y);
            maxZ = Math.max(maxZ, p.z);
        }

        Set<Position> shared = new LinkedHashSet<>();
        int radius = ConfigurationHandler.INSTANCE.getDismantleSharedScanRadius();
        int scanLimit = ConfigurationHandler.INSTANCE.getDismantleSharedScanLimit();
        int inspected = 0;
        // Cover the God Forge's optional rings, as well as the tier selected on this projector.
        Set<Integer> probeTiers = new LinkedHashSet<>();
        probeTiers.add(1);
        probeTiers.add(2);
        probeTiers.add(3);
        probeTiers.add(Math.min(64, Math.max(1, trigger.stackSize)));

        // Copy only the loaded tiles. Never load chunks merely to search for possible owners.
        for (Object candidate : new ArrayList<>(world.loadedTileEntityList)) {
            if (!(candidate instanceof TileEntity)) continue;
            TileEntity tile = (TileEntity) candidate;
            if (tile == primaryController || tile.isInvalid() || tile.getWorldObj() != world
                    || tile.xCoord < minX - radius || tile.xCoord > maxX + radius
                    || tile.yCoord < minY - radius || tile.yCoord > maxY + radius
                    || tile.zCoord < minZ - radius || tile.zCoord > maxZ + radius
                    || !world.blockExists(tile.xCoord, tile.yCoord, tile.zCoord)
                    || world.getTileEntity(tile.xCoord, tile.yCoord, tile.zCoord) != tile) continue;

            // GT multiblock controllers expose mMachine on their meta tile entity. Reading it
            // avoids treating nearby unbuilt controllers as owners of unrelated blocks.
            Object meta = invokeNoArgs(tile, "getMetaTileEntity");
            if (!isFormedGtMultiblock(meta)) continue;
            IConstructable constructable = ConstructableUtility.getConstructable(tile, 2);
            if (constructable == null) continue;
            if (++inspected > scanLimit) {
                StructureLib.LOGGER.warn("Dismantle wall-sharing scan hit the limit of {} controllers", scanLimit);
                return null;
            }

            Position otherController = new Position(tile.xCoord, tile.yCoord, tile.zCoord);
            if (selected.contains(otherController)) shared.add(otherController);

            boolean traversed = false;
            for (int tier : probeTiers) {
                ItemStack variant = trigger.copy();
                variant.stackSize = tier;
                Set<Position> recognized = StructureCapture.collectOverlaps(
                        constructable, variant, world, otherController, selected);
                if (recognized == null) continue;
                traversed = true;
                shared.addAll(recognized);
            }
            if (!traversed) {
                StructureLib.LOGGER.warn(
                        "Dismantle wall-sharing scan could not read formed controller {} at ({}, {}, {})",
                        meta.getClass().getName(), tile.xCoord, tile.yCoord, tile.zCoord);
                return null;
            }
            if (shared.size() == selected.size()) break;
        }

        return shared;
    }

    private static Object invokeNoArgs(Object target, String name) {
        try {
            Method method = target.getClass().getMethod(name);
            return method.invoke(target);
        } catch (ReflectiveOperationException | SecurityException e) {
            return null;
        }
    }

    private static boolean isFormedGtMultiblock(Object meta) {
        if (meta == null) return false;
        try {
            Field formed = meta.getClass().getField("mMachine");
            return formed.getType() == boolean.class && formed.getBoolean(meta);
        } catch (ReflectiveOperationException | SecurityException e) {
            return false;
        }
    }
}
