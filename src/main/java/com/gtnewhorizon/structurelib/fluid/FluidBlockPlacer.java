package com.gtnewhorizon.structurelib.fluid;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;

/**
 * Put a fluid block into the world.
 * <p>
 * The default implementation used by StructureLib is a plain {@code world.setBlock(x, y, z, block, meta, 2)}, which is
 * what every vanilla and Forge fluid block expects. Register a custom placer for a fluid block that needs more than
 * that, e.g. one that keeps its fluid inside a tile entity.
 */
public interface FluidBlockPlacer {

    /**
     * Place the fluid block.
     *
     * @param world world to place in. Never a client side world.
     * @param x     x coord
     * @param y     y coord
     * @param z     z coord
     * @param block block to place
     * @param meta  block meta to place
     * @param fluid the fluid that has been paid for, and how much of it. Never null.
     * @return true if the block has been placed, false otherwise. Autoplace rolls back and refunds nothing when this
     *         returns false before anything was consumed.
     */
    boolean place(World world, int x, int y, int z, Block block, int meta, FluidStack fluid);
}
