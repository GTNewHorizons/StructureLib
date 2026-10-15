package com.gtnewhorizon.structurelib.fluid;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraftforge.fluids.BlockFluidFinite;
import net.minecraftforge.fluids.IFluidBlock;

/**
 * Measure how much fluid a block in the world holds.
 * <p>
 * Fluid blocks disagree on what their meta means. A vanilla style liquid counts down as it thins out, some modded
 * fluids count up instead, and a Forge fluid block can report its own fill. These helpers hide that difference, which
 * is what allows autoplace to charge only for the fluid that is actually missing from a position.
 */
public class FluidAmounts {

    private FluidAmounts() {}

    /**
     * How much fluid a block of given meta holds, assuming it is a fluid block whose meta encodes a level.
     * <p>
     * Blocks implementing Forge's {@code IFluidBlock} answer this themselves once they are in the world, so this is
     * only used to derive a cost for a state that is not in the world yet.
     *
     * @param block      the fluid block. Must not be null.
     * @param meta       the meta to interpret
     * @param fullAmount how much fluid a full block of this fluid holds
     * @return amount of fluid, between 0 and {@code fullAmount} inclusive
     */
    public static int getAmountForMeta(Block block, int meta, int fullAmount) {
        if (block == null) throw new IllegalArgumentException();
        int level = meta & 7;
        // A finite fluid counts up: meta 0 is the thinnest state, meta 7 is a full block.
        if (block instanceof BlockFluidFinite) return fullAmount * (level + 1) / 8;
        // Everything else counts down: meta 0 is a source block, meta 7 is the thinnest state.
        return fullAmount * (8 - level) / 8;
    }

    /**
     * How full the block at given position is, as a fraction of a full block.
     *
     * @return a fraction between 0 and 1 inclusive. 0 when the position does not hold a fluid block.
     */
    public static float getStoredFraction(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        if (block == null) return 0;
        if (block instanceof IFluidBlock fluidBlock) {
            // A negative percentage means the block fills from the top down. Only its magnitude is interesting here.
            return Math.min(1f, Math.abs(fluidBlock.getFilledPercentage(world, x, y, z)));
        }
        if (block.getMaterial() != null && block.getMaterial().isLiquid()) {
            return getAmountForMeta(block, world.getBlockMetadata(x, y, z), 8) / 8f;
        }
        return 0;
    }

    /**
     * How much fluid the block at given position holds.
     *
     * @param capacity how much fluid a full block of this kind holds, i.e. the amount to scale the fill fraction by
     * @return amount of fluid, between 0 and {@code capacity} inclusive
     */
    public static int getStoredAmount(World world, int x, int y, int z, int capacity) {
        return Math.round(getStoredFraction(world, x, y, z) * capacity);
    }
}
