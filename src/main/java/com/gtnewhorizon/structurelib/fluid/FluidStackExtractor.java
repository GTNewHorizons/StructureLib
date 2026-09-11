package com.gtnewhorizon.structurelib.fluid;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/**
 * Extract fluid from a single item stack, e.g. a bucket or a cell.
 * <p>
 * Implementations are registered in {@link FluidStackExtractors}. StructureLib ships implementations for Forge's
 * {@code IFluidContainerItem} and for {@code FluidContainerRegistry}, which together cover the vast majority of fluid
 * containers. Register your own implementation when your containers expose their fluid by other means, e.g. fluid
 * stored inside a backpack or inside a custom NBT layout.
 * <p>
 * All methods must be side effect free when a simulation is requested, and must never depend on a world.
 */
public interface FluidStackExtractor {

    /**
     * Whether this extractor can work with given item stack. This must be a cheap, side effect free check.
     *
     * @param stack the stack to test. never null.
     */
    boolean isValidSource(ItemStack stack);

    /**
     * Extract up to {@code resource.amount} of fluid from this container.
     * <p>
     * The container item itself is never swapped by this method. A container whose fluid is stored in its NBT may
     * mutate that NBT in place when {@code simulate} is false. A container that has to turn into another item, or to
     * disappear, declares so through {@link #convertsContainer(ItemStack, FluidStack)} and
     * {@link #getReplacement(ItemStack)} instead.
     * <p>
     * A container that is consumed as a whole, i.e. one that answers true from
     * {@link #convertsContainer(ItemStack, FluidStack)}, cannot be split, so it may report the whole content of the
     * container even when that is more than was asked for. Such a surplus is spent, as a half emptied bucket does not
     * exist.
     *
     * @param container what to drain from. never null, and always accepted by {@link #isValidSource(ItemStack)}.
     * @param resource  the fluid to drain, and how much of it at most. never null, and never mutated.
     * @param simulate  whether to actually commit the modification. true for dry run, false otherwise.
     * @return amount of fluid drained. never negative, and never more than {@code resource.amount} unless this
     *         container is consumed as a whole
     */
    int drain(ItemStack container, FluidStack resource, boolean simulate);

    /**
     * Whether draining this container would turn it into another item, or make it disappear.
     * <p>
     * Called before draining, with the container still holding its fluid. When this returns true, the caller replaces
     * the whole slot with {@link #getReplacement(ItemStack)} after a successful drain, so the implementation is
     * responsible for the whole stack: either it has to be a container that is drained as a unit, e.g. a bucket, or
     * {@link #drain(ItemStack, FluidStack, boolean)} has to reduce {@code stackSize} itself, as a stack of fluid drops
     * does.
     *
     * @param container the container stack, in its state before being drained
     * @param resource  the fluid that is about to be drained, and how much of it at most
     */
    default boolean convertsContainer(ItemStack container, FluidStack resource) {
        return false;
    }

    /**
     * The item stack that should replace given container once it got drained, or null when the container slot should
     * become empty instead.
     * <p>
     * The caller refuses to drain a container that has to be replaced by another item while its slot holds more than
     * one item, as it cannot hand the replacement back to the player safely. A null replacement is always allowed, as
     * it only empties the slot.
     *
     * @param container the container stack, in its state before being drained
     */
    @Nullable
    default ItemStack getReplacement(ItemStack container) {
        return null;
    }
}
