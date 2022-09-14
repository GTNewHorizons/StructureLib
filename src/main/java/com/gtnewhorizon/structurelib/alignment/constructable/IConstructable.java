package com.gtnewhorizon.structurelib.alignment.constructable;

import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.item.ItemStack;

/**
 * Created by Tec on 24.03.2017.
 */
public interface IConstructable {
    void construct(ItemStack stackSize, boolean hintsOnly);

    /**
     * Get the structure definition used for this constructable. Can be null if this constructable is not backed by one.
     */
    default IStructureDefinition<?> getStructureDefinition() {
        return null;
    }

    @SideOnly(Side.CLIENT)
    String[] getStructureDescription(ItemStack stackSize);
}
