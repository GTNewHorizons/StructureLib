package com.gtnewhorizon.structurelib.item;

import static com.gtnewhorizon.structurelib.StructureLibAPI.MOD_ID;
import static net.minecraft.util.StatCollector.translateToLocal;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.alignment.AlignmentUtility;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemFrontRotationTool extends Item {

    public ItemFrontRotationTool() {
        setMaxStackSize(1);
        setUnlocalizedName("structurelib.frontRotationTool");
        setTextureName(MOD_ID + ":itemFrontRotationTool");
        setCreativeTab(StructureLib.creativeTab);
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
            float hitX, float hitY, float hitZ) {
        return AlignmentUtility.handle(player, world, x, y, z);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean adv) {
        // Tooltip Text:
        //   Triggers Front Rotation Interface
        //   Rotates only the front panel,
        //   which allows structure rotation.

        tooltip.add(translateToLocal("item.structurelib.frontRotationTool.desc.0"));
        tooltip.add(EnumChatFormatting.BLUE + translateToLocal("item.structurelib.frontRotationTool.desc.1"));
        tooltip.add(EnumChatFormatting.BLUE + translateToLocal("item.structurelib.frontRotationTool.desc.2"));
    }
}
