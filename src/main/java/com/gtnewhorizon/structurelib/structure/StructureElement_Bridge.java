package com.gtnewhorizon.structurelib.structure;

import java.util.function.Consumer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

/**
 * Internal bridge class for easing implementation
 */
abstract class StructureElement_Bridge<T> implements IStructureElement<T> {
    // seal the deprecated method to prevent accidental overrides
    @Deprecated
    @Override
    public final PlaceResult survivalPlaceBlock(
            T t,
            World world,
            int x,
            int y,
            int z,
            ItemStack trigger,
            IItemSource s,
            EntityPlayerMP actor,
            Consumer<IChatComponent> chatter) {
        return survivalPlaceBlock(t, world, x, y, z, trigger, AutoPlaceEnvironment.fromLegacy(s, actor, chatter));
    }

    // clear the default implementation. we don't need that.
    public abstract PlaceResult survivalPlaceBlock(
            T t, World world, int x, int y, int z, ItemStack trigger, AutoPlaceEnvironment env);
}
