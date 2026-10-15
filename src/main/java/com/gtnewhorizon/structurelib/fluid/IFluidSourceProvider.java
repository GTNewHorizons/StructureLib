package com.gtnewhorizon.structurelib.fluid;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;

/**
 * Offers a fluid source to ask before the fluid containers a player is carrying.
 * <p>
 * This is how a mod that can hand out fluid without an item in the player's inventory plugs into autoplace, e.g. the ME
 * network behind a wireless fluid terminal the player has on them, or the tanks of a network the player has access to.
 * Providers are registered in {@link FluidSourceProviders}, so the player can reorder or disable them in StructureLib's
 * config like every other registered feature.
 * <p>
 * A provider is only ever asked for a server side player, and only while autoplace is placing a fluid block, so it does
 * not have to be cheap, but it should still avoid work it can do lazily: the returned source is asked for fluid more
 * than once, and should look up whatever it drains from at that point rather than when it is created.
 */
public interface IFluidSourceProvider {

    /**
     * A fluid source for the given player, or null when this provider has nothing to contribute right now.
     *
     * @param player the player the structure is being built for. never null, and never a client side player.
     */
    @Nullable
    IFluidSource getFluidSource(EntityPlayerMP player);
}
