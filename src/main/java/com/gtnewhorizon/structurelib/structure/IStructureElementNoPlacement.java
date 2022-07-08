package com.gtnewhorizon.structurelib.structure;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.UUID;

public interface IStructureElementNoPlacement<T> extends IStructureElement<T> {
    @Override
    default boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
        return false;
    }

    @Override
    default PlaceResult survivalPlaceBlock(T t, World world, int x, int y, int z, ItemStack trigger, IItemSource s, UUID actorProfile) {
        return PlaceResult.SKIP;
    }
}
