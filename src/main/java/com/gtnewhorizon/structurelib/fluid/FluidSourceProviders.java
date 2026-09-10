package com.gtnewhorizon.structurelib.fluid;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;

import com.gtnewhorizon.structurelib.SortedRegistry;

/**
 * The registry of everything that can hand out fluid on a player's behalf, plus the composition that autoplace uses.
 * <p>
 * This mirrors {@link FluidContainerExtractors}: it is a {@link SortedRegistry}, so the player can reorder or disable
 * every provider in StructureLib's config, and the ordering can be synced to the server. Providers are asked in order,
 * and whatever the player is carrying comes last, so a provider that holds a lot of fluid, e.g. an ME network, is
 * drained before the player's buckets are emptied.
 */
public class FluidSourceProviders {

    private static final SortedRegistry<IFluidSourceProvider> PROVIDERS = new SortedRegistry<>("fluidsourceproviders");

    private FluidSourceProviders() {}

    /**
     * Dummy method to force the class to initialize, and with it the registry and its config entry.
     */
    public static void init() {}

    /**
     * Register a provider. Lower keys are asked first.
     *
     * @param key      unique key. Matches the key shown in the config gui.
     * @param provider the provider
     */
    public static void register(String key, IFluidSourceProvider provider) {
        PROVIDERS.register(key, provider);
    }

    /**
     * The fluid source to use for a player: every provider that has something to offer, in their configured order,
     * followed by the fluid containers the player is carrying.
     *
     * @param player the player the structure is being built for
     * @return a source that never returns more than it was asked for. never null.
     */
    public static IFluidSource getSourceFor(EntityPlayerMP player) {
        if (player == null) throw new IllegalArgumentException();
        List<IFluidSource> sources = new ArrayList<>();
        for (IFluidSourceProvider provider : PROVIDERS.getPlayerOrdering(player)) {
            IFluidSource source = provider.getFluidSource(player);
            if (source != null) sources.add(source);
        }
        sources.add(IFluidSource.fromPlayer(player));
        return IFluidSource.composite(sources.toArray(new IFluidSource[0]));
    }
}
