package com.gtnewhorizon.structurelib.dismantle;

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
 * Conservative wall-sharing guard. Protect any occupied primary position included in another loaded constructable
 * controller's structure traversal. A controller can temporarily report itself unformed while still owning shared
 * casings; never use that flag to exclude it.
 */
final class SharedBlockProtection {

    private SharedBlockProtection() {}

    /** @return the protected positions, or null if a nearby constructable cannot be scanned safely. */
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
        // A secondary controller can be on the opposite end of a tall Exo Foundry. Never use
        // an old 48-block config value to silently skip it when the structures overlap.
        int radius = Math.max(128, ConfigurationHandler.INSTANCE.getDismantleSharedScanRadius());
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
            if (tile == primaryController || tile.isInvalid()
                    || tile.getWorldObj() != world
                    || tile.xCoord < minX - radius
                    || tile.xCoord > maxX + radius
                    || tile.yCoord < minY - radius
                    || tile.yCoord > maxY + radius
                    || tile.zCoord < minZ - radius
                    || tile.zCoord > maxZ + radius
                    || !world.blockExists(tile.xCoord, tile.yCoord, tile.zCoord)
                    || world.getTileEntity(tile.xCoord, tile.yCoord, tile.zCoord) != tile)
                continue;

            // Include unformed controllers too: mMachine can be false during structure updates,
            // and using it as a filter caused shared blocks to be destroyed.
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
                Set<Position> recognized = StructureCapture
                        .collectOverlaps(constructable, variant, world, otherController, selected);
                if (recognized == null) continue;
                traversed = true;
                shared.addAll(recognized);
            }
            if (!traversed) {
                StructureLib.LOGGER.warn(
                        "Dismantle wall-sharing scan could not read controller {} at ({}, {}, {})",
                        tile.getClass().getName(),
                        tile.xCoord,
                        tile.yCoord,
                        tile.zCoord);
                return null;
            }
            if (shared.size() == selected.size()) break;
        }

        StructureLib.LOGGER.info(
                "Dismantle wall-sharing scan at ({}, {}, {}): {} nearby constructable controllers, {} protected positions",
                primaryController.xCoord,
                primaryController.yCoord,
                primaryController.zCoord,
                inspected,
                shared.size());
        return shared;
    }
}
