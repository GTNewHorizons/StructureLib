package com.gtnewhorizon.structurelib.fluid;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidHandler;

/**
 * Represent a source of fluid. Take only, cannot be put back. This is the fluid counterpart of the item source that
 * survival autoplace already uses for blocks with an item form.
 * <p>
 * A structure element that has to place a fluid block asks the autoplace environment for a fluid source. When the
 * environment doesn't carry one, StructureLib falls back to {@link #fromPlayer(EntityPlayerMP)} as long as the actor is
 * a server side player, so multiblocks and third party tools that were written before this API existed keep working
 * without any change.
 * <p>
 * Implementations are expected to be side effect free when {@code simulate} is true. A failed simulation followed by a
 * successful one is not guaranteed to succeed, but it is not expected to happen either as autoplace runs on the server
 * thread with no concurrent inventory modification.
 */
public interface IFluidSource {

    /**
     * Retrieve up to {@code resource.amount} of the given fluid from this source.
     * <p>
     * The given resource is never mutated.
     *
     * @param resource what fluid to extract, and how much of it at most. must have a positive amount.
     * @param simulate whether to actually commit the modification. true for dry run, false otherwise.
     * @return a new fluid stack describing what has actually been extracted. Never null. A zero amount means nothing
     *         could be extracted. The returned stack keeps the NBT tag of the requested fluid.
     * @throws IllegalArgumentException if the given resource is null or has no fluid
     */
    @Nonnull
    FluidStack take(FluidStack resource, boolean simulate);

    /**
     * Take exactly {@code resource.amount} amount of fluid. Nothing is taken if the full amount isn't available.
     *
     * @param resource what fluid to extract, and how much of it. must have a positive amount.
     * @param simulate whether to actually commit the modification. true for dry run, false otherwise.
     * @return true if at least this much fluid can be/is taken.
     * @throws IllegalArgumentException if the given resource is null or has no fluid
     */
    default boolean takeAll(FluidStack resource, boolean simulate) {
        if (resource == null || resource.getFluid() == null) throw new IllegalArgumentException();
        if (resource.amount <= 0) return true;
        if (getAvailable(resource) < resource.amount) return false;
        if (simulate) return true;
        FluidStack taken = take(resource, false);
        return taken != null && taken.amount >= resource.amount;
    }

    /**
     * Query how much of the given fluid this source could provide right now.
     *
     * @param resource what fluid to query. must have a positive amount, which acts as the upper bound of the result.
     * @return amount available, between 0 and {@code resource.amount} inclusive
     * @throws IllegalArgumentException if the given resource is null or has no fluid
     */
    default int getAvailable(FluidStack resource) {
        if (resource == null || resource.getFluid() == null) throw new IllegalArgumentException();
        FluidStack taken = take(resource, true);
        return taken == null ? 0 : Math.max(0, Math.min(taken.amount, resource.amount));
    }

    /**
     * A source that never provides anything. Useful as a placeholder for a fluid source that is not available.
     */
    static IFluidSource empty() {
        return (resource, simulate) -> {
            if (resource == null || resource.getFluid() == null) throw new IllegalArgumentException();
            return new FluidStack(resource, 0);
        };
    }

    /**
     * Combine several sources into one, asking them in order until the requested amount is met.
     * <p>
     * This is how a multiblock that wants its fluid to come from somewhere else than the player keeps the player's own
     * containers as a fallback: {@code composite(custom, env.getEffectiveFluidSource())}.
     *
     * @param sources the sources to ask, in order. Null entries are skipped.
     */
    static IFluidSource composite(IFluidSource... sources) {
        if (sources == null || sources.length == 0) return empty();
        return (resource, simulate) -> {
            if (resource == null || resource.getFluid() == null) throw new IllegalArgumentException();
            int remaining = resource.amount;
            for (IFluidSource source : sources) {
                if (source == null || remaining <= 0) continue;
                FluidStack taken = source.take(new FluidStack(resource, remaining), simulate);
                if (taken == null || taken.amount <= 0) continue;
                remaining -= Math.min(remaining, taken.amount);
            }
            return new FluidStack(resource, resource.amount - remaining);
        };
    }

    /**
     * Construct a fluid source from the fluid containers held by given player.
     * <p>
     * This will be backed by {@link FluidStackExtractors}, which covers every fluid container registered with Forge,
     * and every container type registered by another mod through
     * {@link FluidStackExtractors#register(String, FluidStackExtractor)}.
     */
    static IFluidSource fromPlayer(EntityPlayerMP player) {
        return (resource, simulate) -> FluidStackExtractors.takeFromPlayer(player, resource, simulate);
    }

    /**
     * Construct a fluid source from given inventory. Useful for multiblocks that want to drain fluid from a nearby
     * machine instead of from the player.
     * <p>
     * This will be backed by {@link FluidStackExtractors}.
     */
    static IFluidSource fromInventory(IInventory inv) {
        return (resource, simulate) -> FluidStackExtractors.takeFromInventory(inv, resource, simulate);
    }

    /**
     * Construct a fluid source from a Forge fluid handler, e.g. a tank or a machine.
     * <p>
     * This is the intended way for a multiblock to pull fluid from a world container instead of from the player. A
     * multiblock that wants to do so hands the source to autoplace through its own autoplace environment.
     *
     * @param handler the handler to drain from. must not be null.
     * @param sides   sides to try, in order. An empty or null array means every side.
     */
    static IFluidSource fromHandler(IFluidHandler handler, ForgeDirection... sides) {
        if (handler == null) throw new IllegalArgumentException();
        ForgeDirection[] directions = sides == null || sides.length == 0 ? ForgeDirection.VALID_DIRECTIONS
                : sides.clone();
        return (resource, simulate) -> {
            if (resource == null || resource.getFluid() == null) throw new IllegalArgumentException();
            int remaining = resource.amount;
            for (ForgeDirection direction : directions) {
                if (remaining <= 0) break;
                FluidStack drained = handler.drain(direction, new FluidStack(resource, remaining), !simulate);
                if (drained == null || drained.getFluid() != resource.getFluid()) continue;
                remaining -= Math.min(remaining, drained.amount);
            }
            return new FluidStack(resource, resource.amount - remaining);
        };
    }
}
