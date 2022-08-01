package com.gtnewhorizon.structurelib.structure;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

import java.util.function.Consumer;

/**
 * Use StructureUtility to instantiate
 */
interface IStructureNavigate<T> extends IStructureElement<T> {
    @Override
    default boolean check(T t, World world, int x, int y, int z) {
        return true;
    }

    @Override
    default boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
        return true;
    }

    @Override
    default boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
        return true;
    }

    @Override
    default PlaceResult survivalPlaceBlock(
        T t, World world, int x, int y, int z, ItemStack trigger, IItemSource s, EntityPlayerMP actor, Consumer<IChatComponent> chatter) {
        return PlaceResult.SKIP;
    }

    default boolean isNavigating() {
        return true;
    }
}
