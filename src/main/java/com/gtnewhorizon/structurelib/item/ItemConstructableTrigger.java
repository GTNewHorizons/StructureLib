package com.gtnewhorizon.structurelib.item;

import static com.gtnewhorizon.structurelib.StructureLibAPI.MOD_ID;
import static net.minecraft.util.EnumChatFormatting.BLUE;
import static net.minecraft.util.StatCollector.translateToLocal;

import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.alignment.constructable.ConstructableUtility;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class ItemConstructableTrigger extends Item {
    public ItemConstructableTrigger() {
        setUnlocalizedName("structurelib.constructableTrigger");
        setTextureName(MOD_ID + ":itemConstructableTrigger");
        setCreativeTab(StructureLib.creativeTab);
    }

    @Override
    public boolean onItemUseFirst(
            ItemStack stack,
            EntityPlayer player,
            World world,
            int x,
            int y,
            int z,
            int side,
            float hitX,
            float hitY,
            float hitZ) {
        return ConstructableUtility.handle(stack, player, world, x, y, z, side);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addInformation(ItemStack aStack, EntityPlayer ep, List aList, boolean boo) {
        aList.add(
                translateToLocal("item.structurelib.constructableTrigger.desc.0")); // Triggers Constructable Interface
        aList.add(BLUE
                + translateToLocal(
                        "item.structurelib.constructableTrigger.desc.1")); // Shows multiblock construction details,
        aList.add(BLUE
                + translateToLocal(
                        "item.structurelib.constructableTrigger.desc.2")); // just Use on a multiblock controller.
        aList.add(BLUE
                + translateToLocal(
                        "item.structurelib.constructableTrigger.desc.3")); // (Sneak Use in creative to build)
        aList.add(BLUE
                + translateToLocal("item.structurelib.constructableTrigger.desc.4")); // Quantity affects tier/mode/type
    }
}
