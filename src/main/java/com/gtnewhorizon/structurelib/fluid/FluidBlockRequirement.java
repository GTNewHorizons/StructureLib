package com.gtnewhorizon.structurelib.fluid;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraftforge.fluids.FluidStack;

import com.github.bsideup.jabel.Desugar;

/**
 * A concrete request to put one fluid block at one position: which block and meta the structure wants there, what that
 * costs, and how strict the check for an already partially filled position is.
 * <p>
 * Structure elements create one of these, either from the {@link FluidPlacementRegistry} or from their own parameters,
 * and hand it to the autoplace helper.
 *
 * @param block  the block the structure wants at that position
 * @param meta   the block meta the structure wants at that position
 * @param cost   the fluid that has to be paid for a position that holds nothing yet. Must hold a fluid and a positive
 *               amount.
 * @param policy how much fluid an existing position has to hold to be accepted as is
 * @param placer how to actually place the block, or null to place it with a plain block set
 */
@Desugar
public record FluidBlockRequirement(Block block, int meta, FluidStack cost, FluidFillPolicy policy,
        @Nullable FluidBlockPlacer placer) {

    public FluidBlockRequirement {
        if (block == null) throw new IllegalArgumentException("block must not be null");
        if (cost == null || cost.getFluid() == null) throw new IllegalArgumentException("cost must hold a fluid");
        if (cost.amount <= 0) throw new IllegalArgumentException("cost must have a positive amount");
        if (policy == null) throw new IllegalArgumentException("policy must not be null");
    }

    /**
     * Build a requirement from a registered placement.
     *
     * @param placement what the registry knows about this block state
     * @param block     the block the structure wants
     * @param meta      the block meta the structure wants
     * @param policy    how strict the check for an already partially filled position is
     */
    public static FluidBlockRequirement of(FluidBlockPlacement placement, Block block, int meta,
            FluidFillPolicy policy) {
        if (placement == null) throw new IllegalArgumentException("placement must not be null");
        return new FluidBlockRequirement(block, meta, placement.cost(), policy, placement.placer());
    }

    /**
     * Build a requirement that pays with given fluid, and that places the block directly.
     */
    public static FluidBlockRequirement of(Block block, int meta, FluidStack cost, FluidFillPolicy policy) {
        return new FluidBlockRequirement(block, meta, cost, policy, null);
    }
}
