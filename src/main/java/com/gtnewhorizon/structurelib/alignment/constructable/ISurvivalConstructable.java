package com.gtnewhorizon.structurelib.alignment.constructable;

import com.gtnewhorizon.structurelib.structure.IItemSource;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.UUID;

public interface ISurvivalConstructable extends IConstructable {
    /**
     * Construct the structure using {@link com.gtnewhorizon.structurelib.structure.IStructureElement#survivalPlaceBlock(Object, World, int, int, int, ItemStack, IItemSource, net.minecraft.entity.player.EntityPlayerMP)}
     *
     * @return -1 if done, otherwise number of elements placed this round
     */
    int survivalConstruct(ItemStack stackSize, int elementBudget, IItemSource source, EntityPlayerMP actor);
}
