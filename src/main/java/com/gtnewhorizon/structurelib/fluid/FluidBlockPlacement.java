package com.gtnewhorizon.structurelib.fluid;

import javax.annotation.Nullable;

import net.minecraftforge.fluids.FluidStack;

import com.github.bsideup.jabel.Desugar;

/**
 * How a specific block state is placed by autoplace: what fluid it costs, and how to write it into the world.
 * <p>
 * Register one of these through {@link FluidPlacementRegistry} to teach StructureLib about a fluid block that it cannot
 * figure out on its own, e.g. a fluid block that stores its fluid somewhere else, or one whose fluid cost is not the
 * default bucket.
 *
 * @param cost       the fluid that has to be paid for one block of this state. Must hold a fluid and a positive amount.
 * @param forceFluid whether to always pay with fluid, even when this block also has an item form. When false, the item
 *                   form is preferred, which keeps the behaviour of existing multiblocks unchanged.
 * @param placer     how to actually place the block, or null to place it with a plain block set
 */
@Desugar
public record FluidBlockPlacement(FluidStack cost, boolean forceFluid, @Nullable FluidBlockPlacer placer) {

    public FluidBlockPlacement {
        if (cost == null || cost.getFluid() == null) throw new IllegalArgumentException("cost must hold a fluid");
        if (cost.amount <= 0) throw new IllegalArgumentException("cost must have a positive amount");
    }

    /**
     * A placement that pays with given fluid, prefers the item form when there is one, and places the block directly.
     */
    public static FluidBlockPlacement of(FluidStack cost) {
        return new FluidBlockPlacement(cost, false, null);
    }

    /**
     * A placement that pays with given fluid, and always pays with fluid even when the block has an item form.
     */
    public static FluidBlockPlacement forced(FluidStack cost) {
        return new FluidBlockPlacement(cost, true, null);
    }
}
