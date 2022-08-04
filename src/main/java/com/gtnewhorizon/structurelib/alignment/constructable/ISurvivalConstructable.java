package com.gtnewhorizon.structurelib.alignment.constructable;

import com.gtnewhorizon.structurelib.structure.IItemSource;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public interface ISurvivalConstructable extends IConstructable {
    /**
     * Construct the structure using {@link com.gtnewhorizon.structurelib.structure.IStructureElement#survivalPlaceBlock(Object, World, int, int, int, ItemStack, IItemSource, EntityPlayerMP, java.util.function.Consumer)}
     *
     * @param elementBudget The server configured element budget. You can tune this up a bit if the structure is too big, but
     *                      generally should not be a 4 digits number to not overwhelm the server
     * @return -1 if done, otherwise number of elements placed this round
     */
    int survivalConstruct(ItemStack stackSize, int elementBudget, IItemSource source, EntityPlayerMP actor);
}
