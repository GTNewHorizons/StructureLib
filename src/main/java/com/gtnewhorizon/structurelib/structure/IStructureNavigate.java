package com.gtnewhorizon.structurelib.structure;

import java.util.function.Consumer;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

/**
 * Use StructureUtility to instantiate
 */
interface IStructureNavigate extends IStructureElement<Object> {

    @Override
    default boolean check(Object t, World world, int x, int y, int z) {
        return true;
    }

    @Override
    default boolean couldBeValid(Object t, World world, int x, int y, int z, ItemStack trigger) {
        return true;
    }

    @Override
    default boolean spawnHint(Object t, World world, int x, int y, int z, ItemStack trigger) {
        return true;
    }

    @Override
    default boolean placeBlock(Object t, World world, int x, int y, int z, ItemStack trigger) {
        return true;
    }

    @Override
    default PlaceResult survivalPlaceBlock(Object t, World world, int x, int y, int z, ItemStack trigger, IItemSource s,
            EntityPlayerMP actor, Consumer<IChatComponent> chatter) {
        return PlaceResult.SKIP;
    }

    @Override
    default PlaceResult survivalPlaceBlock(Object t, World world, int x, int y, int z, ItemStack trigger,
            AutoPlaceEnvironment env) {
        return PlaceResult.SKIP;
    }

    default boolean isNavigating() {
        return true;
    }
}
