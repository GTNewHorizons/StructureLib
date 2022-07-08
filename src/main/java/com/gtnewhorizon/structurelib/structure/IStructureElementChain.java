package com.gtnewhorizon.structurelib.structure;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.UUID;

/**
 * Use StructureUtility to instantiate
 */
public interface IStructureElementChain<T> extends IStructureElement<T> {
    IStructureElement<T>[] fallbacks();

    @Override
    default boolean check(T t, World world, int x, int y, int z) {
        for (IStructureElement<T> fallback : fallbacks()) {
            if (fallback.check(t, world, x, y, z)) {
                return true;
            }
        }
        return false;
    }

    @Override
    default boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
        for (IStructureElement<T> fallback : fallbacks()) {
            if (fallback.spawnHint(t, world, x, y, z, trigger)) {
                return true;
            }
        }
        return false;
    }

    @Override
    default boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
        for (IStructureElement<T> fallback : fallbacks()) {
            if (fallback.placeBlock(t, world, x, y, z, trigger)) {
                return true;
            }
        }
        return false;
    }

    @Override
    default PlaceResult survivalPlaceBlock(T t, World world, int x, int y, int z, ItemStack trigger, IItemSource s, UUID actorProfile) {
        for (IStructureElement<T> fallback : fallbacks()) {
            PlaceResult result = fallback.survivalPlaceBlock(t, world, x, y, z, trigger, s, actorProfile);
            if (result != PlaceResult.SKIP)
                return result;
        }
        return PlaceResult.SKIP;
    }
}
