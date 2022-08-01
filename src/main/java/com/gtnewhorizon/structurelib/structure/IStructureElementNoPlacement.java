package com.gtnewhorizon.structurelib.structure;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

import java.util.function.Consumer;

public interface IStructureElementNoPlacement<T> extends IStructureElement<T> {
    @Override
    default boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
        return false;
    }

    @Override
    default PlaceResult survivalPlaceBlock(
        T t, World world, int x, int y, int z, ItemStack trigger, IItemSource s, EntityPlayerMP actor, Consumer<IChatComponent> chatter) {
        return PlaceResult.REJECT;
    }

    @Override
    default IStructureElementNoPlacement<T> noPlacement() {
        return this;
    }
}
