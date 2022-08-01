package com.gtnewhorizon.structurelib.structure;

import java.util.function.Consumer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

public interface IStructureElementCheckOnly<T> extends IStructureElement<T> {
    @Override
    default boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
        return false;
    }

    @Override
    default boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
        return false;
    }

    @Override
    default PlaceResult survivalPlaceBlock(
            T t,
            World world,
            int x,
            int y,
            int z,
            ItemStack trigger,
            IItemSource s,
            EntityPlayerMP actor,
            Consumer<IChatComponent> chatter) {
        return PlaceResult.SKIP;
    }
}
